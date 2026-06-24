package com.burty.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.adapter.out.http.ResilientHttpExecutor;
import com.burty.adapter.out.store.InMemoryTokenStore;
import com.burty.config.ExternalFinanceProperties;
import com.burty.core.exception.BusinessException;
import com.burty.util.FieldEncryptor;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class OpenBankingApiAdapterWireMockTests {

  private WireMockServer wireMock;
  private OpenBankingApiAdapter adapter;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(0);
    wireMock.start();

    ExternalFinanceProperties properties = new ExternalFinanceProperties();
    properties.setStubMode(false);
    properties.setOpenBankingAccountsUrl(wireMock.baseUrl() + "/accounts");
    properties.setOpenBankingAccessToken("access-token");

    adapter =
        new OpenBankingApiAdapter(
            new RestTemplate(),
            properties,
            new InMemoryTokenStore(new FieldEncryptor("test-field-encryption-key-32bytes!!")),
            new ResilientHttpExecutor(CircuitBreakerRegistry.ofDefaults()));
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void throwsWhenRealApiReturnsEmptyBody() {
    wireMock.stubFor(get(urlPathEqualTo("/accounts")).willReturn(aResponse().withStatus(204)));

    BusinessException error =
        assertThrows(BusinessException.class, () -> adapter.getAccounts("user-1"));
    assertTrue(error.getMessage().contains("empty response"));
  }
}
