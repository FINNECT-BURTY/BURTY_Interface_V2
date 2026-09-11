package com.burty;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.burty.adapter.out.mydata.MyDataOAuthAdapter;
import com.burty.adapter.out.mydata.standard.MyDataTranId;
import com.burty.adapter.out.store.TokenStore;
import com.burty.config.MyDataProperties;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * 정보제공자 토큰 폐기 요청의 모양을 못 박는다.
 *
 * <p>로컬에서 지워도 토큰은 기관 쪽에서 유효하다. 폐기 요청이 규격과 다르게 나가면 기관은 거절하고, 우리는 폐기했다고 믿게 된다.
 */
class MyDataProviderRevokeTests {

  private static final String REVOKE_URL = "https://provider.test/oauth/2.0/revoke";
  private static final String TRAN_ID = "^BURTYMYD01M[0-9A-Z]{14}$";

  private MyDataProperties properties;
  private MockRestServiceServer provider;
  private MyDataOAuthAdapter adapter;

  @BeforeEach
  void setUp() {
    properties = new MyDataProperties();
    properties.setStubMode(false);
    properties.setRevokeUrl(REVOKE_URL);
    properties.setClientId("burty-client");
    properties.setClientSecret("burty-secret");
    RestTemplate restTemplate = new RestTemplate();
    provider = MockRestServiceServer.bindTo(restTemplate).build();
    adapter = new MyDataOAuthAdapter(restTemplate, properties, mock(TokenStore.class));
  }

  @Test
  @DisplayName("폐기 요청은 거래고유번호와 기관코드·토큰·클라이언트 자격을 form 으로 보낸다")
  void revokeRequestFollowsStandardShape() {
    provider
        .expect(requestTo(REVOKE_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(MyDataTranId.HEADER, matchesPattern(TRAN_ID)))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(
            content()
                .formDataContains(
                    Map.of(
                        "org_code", "KB",
                        "token", "rt-1",
                        "client_id", "burty-client",
                        "client_secret", "burty-secret")))
        .andRespond(
            withSuccess(
                "{\"rsp_code\":\"00000\",\"rsp_msg\":\"정상처리\"}", MediaType.APPLICATION_JSON));

    assertDoesNotThrow(() -> adapter.revokeGrant("KB", "rt-1"));
    provider.verify();
  }

  @Test
  @DisplayName("HTTP 200 이어도 응답코드가 정상이 아니면 폐기되지 않은 것으로 본다")
  void nonSuccessResponseCodeIsFailure() {
    // 표준 API 는 HTTP 상태와 별개로 rsp_code 로 결과를 준다. 200 만 보고 성공 처리하면
    // 기관이 거절한 폐기를 완료로 기록한다.
    provider
        .expect(requestTo(REVOKE_URL))
        .andRespond(
            withSuccess(
                "{\"rsp_code\":\"40101\",\"rsp_msg\":\"유효하지 않은 토큰\"}", MediaType.APPLICATION_JSON));

    assertThrows(IllegalStateException.class, () -> adapter.revokeGrant("KB", "rt-1"));
  }

  @Test
  @DisplayName("모의 모드에서는 외부로 폐기 요청을 보내지 않는다")
  void stubModeSendsNothing() {
    properties.setStubMode(true);

    adapter.revokeGrant("KB", "rt-1");

    provider.verify(); // 기대한 요청이 없으므로, 하나라도 나갔으면 여기서 실패한다
  }

  @Test
  @DisplayName("거래고유번호는 요청마다 다르고 형식을 지킨다")
  void tranIdIsUniquePerRequest() {
    // 재사용하면 기관 로그와 우리 로그를 요청 단위로 맞춰볼 수 없다.
    String first = MyDataTranId.next("BURTYMYD01");
    String second = MyDataTranId.next("BURTYMYD01");

    assertTrue(first.matches(TRAN_ID), first);
    assertNotEquals(first, second);
    assertThrows(IllegalArgumentException.class, () -> MyDataTranId.next("SHORT"));
  }
}
