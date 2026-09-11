package com.burty;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.mydata.MyDataBankApiAdapter;
import com.burty.adapter.out.mydata.standard.MyDataTranId;
import com.burty.application.port.out.mydata.MyDataApiException;
import com.burty.application.port.out.mydata.MyDataUnauthorizedException;
import com.burty.config.MyDataProperties;
import com.burty.domain.mydata.model.BankAccount;
import com.burty.domain.mydata.model.BankTransaction;
import com.burty.domain.mydata.model.BankTransaction.Direction;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * 은행 표준 API 호출의 모양과 응답 해석을 못 박는다.
 *
 * <p>규격과 다르게 부르면 기관은 거절하고, 응답을 잘못 읽으면 합계가 틀린다. 둘 다 실제 기관에 붙기 전에는 드러나지 않는다.
 */
class MyDataBankApiAdapterTests {

  private static final String BASE = "https://bank.test/mydata";
  private static final String TRAN_ID = "^BURTYMYD01M[0-9A-Z]{14}$";
  private static final BankAccount ACCOUNT =
      new BankAccount("1002001", "1", "입출금", "1001", "01", true);

  private MyDataProperties properties;
  private MockRestServiceServer provider;
  private MyDataBankApiAdapter adapter;

  @BeforeEach
  void setUp() {
    properties = new MyDataProperties();
    properties.setStubMode(false);
    properties.setApiBaseUrl(BASE);
    RestTemplate restTemplate = new RestTemplate();
    provider = MockRestServiceServer.bindTo(restTemplate).build();
    adapter =
        new MyDataBankApiAdapter(
            restTemplate,
            properties,
            new ResilientHttpExecutor(CircuitBreakerRegistry.ofDefaults()));
  }

  @Test
  @DisplayName("계좌목록은 표준 헤더로 부르고 next_page 를 끝까지 따라간다 — 사용자 ID 는 보내지 않는다")
  void listAccountsFollowsStandardAndPaginates() {
    // 첫 요청은 URL 을 통째로 비교한다. 예전 어댑터는 ?userId=<내부 ID> 를 외부 기관에 실었다.
    provider
        .expect(requestTo(BASE + "/v1/bank/accounts?org_code=KB&search_timestamp=0&limit=100"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer at-1"))
        .andExpect(header(MyDataTranId.HEADER, matchesPattern(TRAN_ID)))
        .andExpect(header("x-api-type", "user-search"))
        .andRespond(
            withSuccess(
                """
                {"rsp_code":"00000","rsp_msg":"정상","next_page":"p2","account_cnt":1,
                 "account_list":[{"account_num":"1002001","is_consent":true,"seqno":"1",
                   "prod_name":"입출금","account_type":"1001","account_status":"01"}]}
                """,
                MediaType.APPLICATION_JSON));
    provider
        .expect(
            requestTo(
                BASE + "/v1/bank/accounts?org_code=KB&search_timestamp=0&limit=100&next_page=p2"))
        .andRespond(
            withSuccess(
                """
                {"rsp_code":"00000","rsp_msg":"정상","account_cnt":1,
                 "account_list":[{"account_num":"2002001","is_consent":false,"seqno":"1",
                   "prod_name":"적금","account_type":"1003","account_status":"01"}]}
                """,
                MediaType.APPLICATION_JSON));

    List<BankAccount> accounts = adapter.listAccounts("KB", "at-1");

    provider.verify();
    assertEquals(2, accounts.size());
    assertTrue(accounts.get(0).consented());
    assertFalse(accounts.get(1).consented(), "is_consent=false 를 동의로 읽었다");
  }

  @Test
  @DisplayName("HTTP 200 이어도 rsp_code 가 정상이 아니면 실패다")
  void nonSuccessResponseCodeFails() {
    provider
        .expect(requestTo(startsWith(BASE + "/v1/bank/accounts")))
        .andRespond(
            withSuccess(
                "{\"rsp_code\":\"40301\",\"rsp_msg\":\"권한 없음\"}", MediaType.APPLICATION_JSON));

    MyDataApiException error =
        assertThrows(MyDataApiException.class, () -> adapter.listAccounts("KB", "at-1"));
    assertEquals("40301", error.responseCode());
  }

  @Test
  @DisplayName("401 은 재발급으로 풀 수 있는 실패로 알린다")
  void unauthorizedIsDistinguished() {
    // 다른 실패와 섞으면 호출부가 재발급을 시도할 근거가 없다.
    provider.expect(requestTo(startsWith(BASE))).andRespond(withUnauthorizedRequest());

    assertThrows(MyDataUnauthorizedException.class, () -> adapter.listAccounts("KB", "expired"));
  }

  @Test
  @DisplayName("잔액은 원화만 더한다")
  void balanceSumsKrwOnly() {
    provider
        .expect(requestTo(BASE + "/v1/bank/accounts/deposit/detail"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json("{\"org_code\":\"KB\",\"account_num\":\"1002001\",\"seqno\":\"1\"}", false))
        .andRespond(
            withSuccess(
                """
                {"rsp_code":"00000","rsp_msg":"정상","detail_cnt":2,"detail_list":[
                  {"currency_code":"KRW","balance_amt":1000000},
                  {"currency_code":"USD","balance_amt":500}]}
                """,
                MediaType.APPLICATION_JSON));

    // 외화를 환율 없이 더하면 합계가 뜻을 잃는다.
    assertEquals(
        0, new BigDecimal("1000000").compareTo(adapter.depositBalance("KB", "at-1", ACCOUNT)));
  }

  @Test
  @DisplayName("거래내역은 기간을 yyyyMMdd 로 보내고 입금·출금을 가른다")
  void transactionsParseDirection() {
    provider
        .expect(requestTo(BASE + "/v1/bank/accounts/deposit/transactions"))
        .andExpect(
            content()
                .json(
                    "{\"org_code\":\"KB\",\"account_num\":\"1002001\",\"from_date\":\"20260601\","
                        + "\"to_date\":\"20260615\",\"limit\":100}",
                    false))
        .andRespond(
            withSuccess(
                """
                {"rsp_code":"00000","rsp_msg":"정상","trans_cnt":3,"trans_list":[
                  {"trans_dtime":"20260601090000","trans_no":"1","trans_type":"01","trans_class":"급여",
                   "currency_code":"KRW","trans_amt":3000000,"balance_amt":3500000},
                  {"trans_dtime":"20260603120000","trans_no":"2","trans_type":"02","trans_class":"카드",
                   "currency_code":"KRW","trans_amt":45000,"balance_amt":3455000},
                  {"trans_dtime":"20260604120000","trans_no":"3","trans_type":"99","trans_class":"기타",
                   "currency_code":"KRW","trans_amt":1000,"balance_amt":3454000}]}
                """,
                MediaType.APPLICATION_JSON));

    List<BankTransaction> transactions =
        adapter.depositTransactions(
            "KB", "at-1", ACCOUNT, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15));

    assertEquals(
        List.of(Direction.DEPOSIT, Direction.WITHDRAWAL, Direction.OTHER),
        transactions.stream().map(BankTransaction::direction).toList());
    assertEquals(LocalDate.of(2026, 6, 3), transactions.get(1).occurredAt().toLocalDate());
  }

  @Test
  @DisplayName("기관이 next_page 를 끝없이 주면 조용히 자르지 않고 실패한다")
  void endlessPaginationFails() {
    // 일부만 더한 합계가 전체처럼 보이면 안 된다.
    provider
        .expect(ExpectedCount.manyTimes(), requestTo(startsWith(BASE + "/v1/bank/accounts")))
        .andRespond(
            withSuccess(
                "{\"rsp_code\":\"00000\",\"next_page\":\"again\",\"account_list\":[]}",
                MediaType.APPLICATION_JSON));

    MyDataApiException error =
        assertThrows(MyDataApiException.class, () -> adapter.listAccounts("KB", "at-1"));
    assertEquals("PAGE_LIMIT", error.responseCode());
  }

  @Test
  @DisplayName("모의 모드도 같은 해석·페이지 순회를 타며 외부로 나가지 않는다")
  void stubModeUsesSamePathWithoutHttp() {
    properties.setStubMode(true);

    List<BankAccount> accounts = adapter.listAccounts("KB", "any");
    List<BankTransaction> transactions =
        adapter.depositTransactions(
            "KB", "any", accounts.get(0), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31));

    provider.verify(); // 기대한 요청이 없으므로 하나라도 나갔으면 실패한다
    assertEquals(2, accounts.size(), "모의 계좌 두 쪽을 다 읽지 못했다");
    assertTrue(transactions.size() > 20, "모의 거래내역이 여러 쪽에 걸쳐 모두 읽히지 않았다");
  }
}
