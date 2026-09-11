package com.burty.adapter.out.mydata;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.mydata.standard.BankAccountListResponse;
import com.burty.adapter.out.mydata.standard.BankDepositDetailResponse;
import com.burty.adapter.out.mydata.standard.BankTransactionListResponse;
import com.burty.adapter.out.mydata.standard.MyDataTranId;
import com.burty.adapter.out.mydata.standard.StandardBankFixtures;
import com.burty.adapter.out.mydata.standard.StandardResponse;
import com.burty.application.port.out.mydata.MyDataApiException;
import com.burty.application.port.out.mydata.MyDataBankPort;
import com.burty.application.port.out.mydata.MyDataUnauthorizedException;
import com.burty.config.MyDataProperties;
import com.burty.domain.mydata.model.BankAccount;
import com.burty.domain.mydata.model.BankTransaction;
import com.burty.domain.mydata.model.BankTransaction.Direction;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 은행 정보제공자 표준 API 어댑터.
 *
 * <p>모든 요청에 접근토큰(Bearer), 거래고유번호({@code x-api-tran-id}), API 유형({@code x-api-type})을 싣는다. HTTP 200
 * 이어도 {@code rsp_code} 가 {@code 00000} 이 아니면 실패다. 목록은 {@code next_page} 가 빌 때까지 따라간다.
 *
 * <p>예전 어댑터는 {@code assetUrl?userId=<내부 사용자 ID>} 를 불렀다. 내부 식별자를 외부 기관 URL 에 싣는 것이고, 표준에서 정보주체는
 * 접근토큰으로 식별된다. 여기서는 사용자 ID 를 기관에 보내지 않는다.
 *
 * <p>모의 모드는 HTTP 만 건너뛴다. 응답은 {@link StandardBankFixtures} 가 같은 형태로 만들고, 해석·순회·판정은 실제와 같은 코드가 한다.
 *
 * <p>거래유형은 {@code 01}(입금)과 {@code 02}(출금)만 방향으로 가른다. 나머지는 지출 합계에 넣지 않는다.
 */
@Component
public class MyDataBankApiAdapter implements MyDataBankPort {

  static final int PAGE_LIMIT = 100;

  /** 기관이 {@code next_page} 를 끝없이 주면 여기서 멈춘다. 조용히 자르지 않고 실패로 올린다 — 일부만 더한 합계가 전체처럼 보이면 안 된다. */
  static final int MAX_PAGES = 50;

  /** 정보주체가 앱에서 조회를 요청한 비정기적 전송. */
  static final String API_TYPE = "user-search";

  private static final String KRW = "KRW";
  private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private static final DateTimeFormatter DTIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final RestTemplate restTemplate;
  private final MyDataProperties properties;
  private final ResilientHttpExecutor resilientHttpExecutor;

  public MyDataBankApiAdapter(
      RestTemplate restTemplate,
      MyDataProperties properties,
      ResilientHttpExecutor resilientHttpExecutor) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.resilientHttpExecutor = resilientHttpExecutor;
  }

  @Override
  public List<BankAccount> listAccounts(String orgCode, String accessToken) {
    List<BankAccount> accounts = new ArrayList<>();
    String nextPage = null;
    for (int page = 0; page < MAX_PAGES; page++) {
      BankAccountListResponse response =
          requireSuccess(accountsPage(orgCode, accessToken, nextPage));
      for (BankAccountListResponse.Account account : orEmpty(response.accountList())) {
        accounts.add(
            new BankAccount(
                account.accountNum(),
                account.seqno(),
                account.prodName(),
                account.accountType(),
                account.accountStatus(),
                Boolean.TRUE.equals(account.isConsent())));
      }
      nextPage = response.nextPage();
      if (isBlank(nextPage)) {
        return accounts;
      }
    }
    throw new MyDataApiException("PAGE_LIMIT", "계좌목록이 " + MAX_PAGES + "쪽을 넘었다");
  }

  @Override
  public BigDecimal depositBalance(String orgCode, String accessToken, BankAccount account) {
    BankDepositDetailResponse response = requireSuccess(detail(orgCode, accessToken, account));
    BigDecimal sum = BigDecimal.ZERO;
    for (BankDepositDetailResponse.Detail detail : orEmpty(response.detailList())) {
      // 외화 잔액은 환율 없이 원화와 더할 수 없다.
      if (isKrw(detail.currencyCode()) && detail.balanceAmt() != null) {
        sum = sum.add(detail.balanceAmt());
      }
    }
    return sum;
  }

  @Override
  public List<BankTransaction> depositTransactions(
      String orgCode, String accessToken, BankAccount account, LocalDate from, LocalDate to) {
    List<BankTransaction> transactions = new ArrayList<>();
    String nextPage = null;
    for (int page = 0; page < MAX_PAGES; page++) {
      BankTransactionListResponse response =
          requireSuccess(transactionsPage(orgCode, accessToken, account, from, to, nextPage));
      for (BankTransactionListResponse.Transaction transaction : orEmpty(response.transList())) {
        if (isKrw(transaction.currencyCode())) {
          transactions.add(toTransaction(transaction));
        }
      }
      nextPage = response.nextPage();
      if (isBlank(nextPage)) {
        return transactions;
      }
    }
    throw new MyDataApiException("PAGE_LIMIT", "거래내역이 " + MAX_PAGES + "쪽을 넘었다");
  }

  private BankAccountListResponse accountsPage(String orgCode, String token, String nextPage) {
    if (properties.isStubMode()) {
      return StandardBankFixtures.accounts(orgCode, nextPage);
    }
    UriComponentsBuilder uri =
        UriComponentsBuilder.fromUriString(properties.getApiBaseUrl() + "/v1/bank/accounts")
            .queryParam("org_code", orgCode)
            // 증분 조회는 쓰지 않는다. 0 은 전체 조회다.
            .queryParam("search_timestamp", 0)
            .queryParam("limit", PAGE_LIMIT);
    if (!isBlank(nextPage)) {
      uri.queryParam("next_page", nextPage);
    }
    return call(
        uri.encode().build().toUri(), HttpMethod.GET, null, token, BankAccountListResponse.class);
  }

  private BankDepositDetailResponse detail(String orgCode, String token, BankAccount account) {
    if (properties.isStubMode()) {
      return StandardBankFixtures.detail(orgCode, account.accountNum());
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("org_code", orgCode);
    body.put("account_num", account.accountNum());
    body.put("seqno", account.seqno());
    body.put("search_timestamp", 0);
    return call(
        URI.create(properties.getApiBaseUrl() + "/v1/bank/accounts/deposit/detail"),
        HttpMethod.POST,
        body,
        token,
        BankDepositDetailResponse.class);
  }

  private BankTransactionListResponse transactionsPage(
      String orgCode,
      String token,
      BankAccount account,
      LocalDate from,
      LocalDate to,
      String nextPage) {
    if (properties.isStubMode()) {
      return StandardBankFixtures.transactions(orgCode, account.accountNum(), from, to, nextPage);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("org_code", orgCode);
    body.put("account_num", account.accountNum());
    body.put("seqno", account.seqno());
    body.put("from_date", from.format(DATE));
    body.put("to_date", to.format(DATE));
    body.put("limit", PAGE_LIMIT);
    if (!isBlank(nextPage)) {
      body.put("next_page", nextPage);
    }
    return call(
        URI.create(properties.getApiBaseUrl() + "/v1/bank/accounts/deposit/transactions"),
        HttpMethod.POST,
        body,
        token,
        BankTransactionListResponse.class);
  }

  private <T> T call(URI uri, HttpMethod method, Object body, String token, Class<T> type) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set(MyDataTranId.HEADER, MyDataTranId.next(properties.getOrgCode()));
    headers.set("x-api-type", API_TYPE);
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    HttpEntity<Object> entity = new HttpEntity<>(body, headers);
    return resilientHttpExecutor.execute(
        "mydata",
        () -> {
          try {
            return restTemplate.exchange(uri, method, entity, type).getBody();
          } catch (HttpClientErrorException.Unauthorized e) {
            throw new MyDataUnauthorizedException("정보제공자가 접근토큰을 거절했다");
          } catch (RestClientResponseException e) {
            throw new MyDataApiException(
                "HTTP_" + e.getStatusCode().value(), String.valueOf(e.getStatusText()));
          }
        });
  }

  private static <T extends StandardResponse> T requireSuccess(T response) {
    if (response == null) {
      throw new MyDataApiException("EMPTY", "응답 본문이 없다");
    }
    if (!response.succeeded()) {
      throw new MyDataApiException(
          String.valueOf(response.rspCode()), String.valueOf(response.rspMsg()));
    }
    return response;
  }

  private static BankTransaction toTransaction(BankTransactionListResponse.Transaction source) {
    Direction direction =
        switch (source.transType() == null ? "" : source.transType()) {
          case "01" -> Direction.DEPOSIT;
          case "02" -> Direction.WITHDRAWAL;
          default -> Direction.OTHER;
        };
    return new BankTransaction(
        LocalDateTime.parse(source.transDtime(), DTIME),
        source.transNo(),
        direction,
        source.transClass(),
        source.transAmt() == null ? BigDecimal.ZERO : source.transAmt().abs(),
        source.balanceAmt());
  }

  private static boolean isKrw(String currencyCode) {
    return currencyCode == null || currencyCode.isBlank() || KRW.equalsIgnoreCase(currencyCode);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static <T> List<T> orEmpty(List<T> list) {
    return list == null ? List.of() : list;
  }
}
