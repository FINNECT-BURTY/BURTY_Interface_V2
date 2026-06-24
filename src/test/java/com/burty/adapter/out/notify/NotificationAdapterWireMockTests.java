package com.burty.adapter.out.notify;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.burty.application.service.notification.NotificationRecipientResolver;
import com.burty.config.NotifyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationAdapterWireMockTests {

  @Mock private NotificationRecipientResolver recipientResolver;

  private WireMockServer wireMock;

  @BeforeEach
  void setUp() {
    wireMock = new WireMockServer(0);
    wireMock.start();
  }

  @AfterEach
  void tearDown() {
    wireMock.stop();
  }

  @Test
  void smsUsesSolapiHmacAuthorizationHeader() {
    NotifyProperties properties = new NotifyProperties();
    properties.getSms().setStubMode(false);
    properties.getSms().setApiUrl(wireMock.baseUrl() + "/messages/v4/send");
    properties.getSms().setApiKey("test-api-key");
    properties.getSms().setApiSecret("test-api-secret");
    properties.getSms().setSenderNumber("01012345678");
    when(recipientResolver.resolvePhone("user-1")).thenReturn(Optional.of("01098765432"));

    wireMock.stubFor(
        post(urlEqualTo("/messages/v4/send"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"statusCode\":\"2000\"}")));

    SmsNotificationAdapter adapter =
        new SmsNotificationAdapter(properties, new RestTemplate(), recipientResolver);
    assertTrue(adapter.send("user-1", "title", "body"));

    LoggedRequest request =
        wireMock.findAll(postRequestedFor(urlEqualTo("/messages/v4/send"))).get(0);
    String authorization = request.getHeader("Authorization");
    assertTrue(authorization.startsWith("HMAC-SHA256 ApiKey=test-api-key"));
    assertTrue(authorization.contains("Signature="));
    assertTrue(!authorization.contains("Signature=test-api-secret"));
  }

  @Test
  void solapiSignatureMatchesHmacSha256() {
    String signature = SolapiAuthHeaderBuilder.sign("secret", "2026-01-01T00:00:00Z" + "salt");
    assertEquals(64, signature.length());
  }

  @Test
  void fcmOAuthProviderFetchesAccessToken() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    String privateKeyPem =
        "-----BEGIN PRIVATE KEY-----\\n"
            + Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
            + "\\n-----END PRIVATE KEY-----";

    wireMock.stubFor(
        post(urlEqualTo("/token"))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"oauth-access-token\",\"expires_in\":3600}")));

    String credentialsJson =
        """
        {
          "type": "service_account",
          "project_id": "burty-prod",
          "private_key": "%s",
          "client_email": "firebase-adminsdk@burty-prod.iam.gserviceaccount.com",
          "token_uri": "%s"
        }
        """
            .formatted(privateKeyPem, wireMock.baseUrl() + "/token");

    FcmOAuthTokenProvider tokenProvider =
        new FcmOAuthTokenProvider(new RestTemplate(), new ObjectMapper());
    String token = tokenProvider.getAccessToken(credentialsJson);
    assertEquals("oauth-access-token", token);
    wireMock.verify(postRequestedFor(urlEqualTo("/token")));
  }
}
