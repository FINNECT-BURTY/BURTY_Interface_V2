package com.burty.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.store.InMemoryTokenStore;
import com.burty.config.ExternalFinanceProperties;
import com.burty.core.exception.BusinessException;
import com.burty.core.exception.ExternalCallUnresolvedException;
import com.burty.util.FieldEncryptor;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 이체 호출의 실패 모드 계약.
 *
 * <p>이 어댑터가 어떤 예외를 던지느냐에 따라 <b>돈의 처리가 갈린다.</b>
 *
 * <ul>
 *   <li>{@link ExternalCallUnresolvedException} → 출금됐을 수도 있다. 주문을 {@code UNKNOWN} 으로 두고 한도를 되돌리지
 *       않는다. 정산 배치가 은행 원장과 대조한다.
 *   <li>그 밖의 예외 → 은행이 명확히 거절했다. 주문을 실패로 확정하고 한도를 되돌린다.
 * </ul>
 *
 * <p>둘을 뒤바꾸면 실제 손해가 난다. 타임아웃을 명확한 거절로 처리하면 <b>돈은 나갔는데 한도는 복구되고 사용자에게는 실패라고 알린다.</b> 반대로 거절을 불명으로
 * 처리하면 멀쩡한 실패 건이 정산 대기로 쌓여 사람 손을 탄다.
 *
 * <p>stub 응답으로는 이 구분을 확인할 수 없다. 실제 HTTP 동작(타임아웃, 연결 끊김, 5xx, 4xx)에 걸어야 한다.
 */
class OpenBankingTransferContractTests {

  private static final String PATH = "/transfers";
  private static final String USER = "user-1";
  private static final String IDEMPOTENCY_KEY = "idem-1";

  private WireMockServer wireMock;
  private OpenBankingApiAdapter adapter;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(0);
    wireMock.start();

    ExternalFinanceProperties properties = new ExternalFinanceProperties();
    properties.setStubMode(false);
    properties.setOpenBankingTransferUrl(wireMock.baseUrl() + PATH);
    properties.setOpenBankingAccessToken("access-token");

    // 타임아웃을 짧게 잡아야 시험이 오래 걸리지 않는다.
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(500));
    factory.setReadTimeout(Duration.ofMillis(500));
    RestTemplate restTemplate = new RestTemplate(factory);

    adapter =
        new OpenBankingApiAdapter(
            restTemplate,
            properties,
            new InMemoryTokenStore(
                new FieldEncryptor("test-field-encryption-key-32bytes!!", 2, "", 0)),
            new ResilientHttpExecutor(CircuitBreakerRegistry.ofDefaults()));
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  @DisplayName("정상 응답은 거래번호와 함께 돌려준다")
  void returnsTransactionIdOnSuccess() {
    wireMock.stubFor(
        post(urlPathEqualTo(PATH))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"rsp_code\":\"A0000\",\"api_tran_id\":\"BANK-TXN-1\"}")));

    Map<String, Object> response = transfer();

    assertNotNull(response);
    assertEquals("BANK-TXN-1", response.get("api_tran_id"));
  }

  @Test
  @DisplayName("읽기 타임아웃은 '결과 불명' 이다 — 출금됐을 수 있다")
  void readTimeoutIsUnresolved() {
    wireMock.stubFor(
        post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(200).withFixedDelay(3_000)));

    // 여기서 일반 예외를 던지면 호출자가 한도를 되돌린다 — 돈은 나갔는데 한도만 복구된다.
    assertThrows(ExternalCallUnresolvedException.class, this::transfer);
  }

  @Test
  @DisplayName("연결이 끊겨도 '결과 불명' 이다")
  void connectionResetIsUnresolved() {
    wireMock.stubFor(
        post(urlPathEqualTo(PATH))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThrows(ExternalCallUnresolvedException.class, this::transfer);
  }

  @Test
  @DisplayName("은행 5xx 는 '결과 불명' 이다 — 처리 중이었을 수 있다")
  void serverErrorIsUnresolved() {
    wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThrows(ExternalCallUnresolvedException.class, this::transfer);
  }

  @Test
  @DisplayName("은행 4xx 는 명확한 거절이다 — 출금이 없었으므로 한도를 되돌려도 된다")
  void clientErrorIsDefiniteFailure() {
    wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(400)));

    BusinessException error = assertThrows(BusinessException.class, this::transfer);
    assertTrue(error.getMessage().contains("400"), "상태코드가 메시지에 없다: " + error.getMessage());
  }

  @Test
  @DisplayName("이체는 재시도하지 않는다 — 은행에 두 번 요청하면 안 된다")
  void mutatingCallIsNotRetried() {
    wireMock.stubFor(post(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

    assertThrows(ExternalCallUnresolvedException.class, this::transfer);

    // 조회는 재시도해도 되지만 이체는 아니다. 재시도가 붙으면 중복 이체가 된다.
    wireMock.verify(1, postRequestedFor(urlPathEqualTo(PATH)));
  }

  @Test
  @DisplayName("같은 멱등키는 같은 은행 거래번호를 만든다")
  void bankTransactionIdIsDeterministic() {
    wireMock.stubFor(
        post(urlPathEqualTo(PATH))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"rsp_code\":\"A0000\",\"api_tran_id\":\"BANK-TXN-1\"}")));

    transfer();
    transfer();

    // 시도마다 다른 거래번호를 보내면 은행이 중복을 잡을 수 없다.
    var requests = wireMock.findAll(postRequestedFor(urlPathEqualTo(PATH)));
    assertEquals(2, requests.size());
    assertEquals(
        bankTranIdOf(requests.get(0).getBodyAsString()),
        bankTranIdOf(requests.get(1).getBodyAsString()),
        "같은 멱등키인데 은행 거래번호가 달라졌다 — 은행이 중복을 잡지 못한다");
  }

  private Map<String, Object> transfer() {
    return adapter.transfer(USER, "1234567890", "9876543210", 10_000L, IDEMPOTENCY_KEY);
  }

  private static String bankTranIdOf(String body) {
    int i = body.indexOf("bankTranId");
    assertTrue(i >= 0, "요청 본문에 bankTranId 가 없다: " + body);
    int start = body.indexOf('"', body.indexOf(':', i)) + 1;
    return body.substring(start, body.indexOf('"', start));
  }
}
