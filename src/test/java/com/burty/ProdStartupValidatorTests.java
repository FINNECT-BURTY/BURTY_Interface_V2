package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.config.BurtyApiProperties;
import com.burty.config.BurtySecurityProperties;
import com.burty.config.ExternalFinanceProperties;
import com.burty.config.IdentityProperties;
import com.burty.config.MyDataProperties;
import com.burty.config.NotifyProperties;
import com.burty.config.ProdStartupValidator;
import com.burty.config.SocialLoginProperties;
import com.burty.config.VoiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class ProdStartupValidatorTests {

  private MockEnvironment mockEnvironment;
  private MyDataProperties myDataProperties;
  private SocialLoginProperties socialLoginProperties;
  private ExternalFinanceProperties externalFinanceProperties;
  private BurtyApiProperties apiProperties;
  private IdentityProperties identityProperties;
  private NotifyProperties notifyProperties;
  private BurtySecurityProperties securityProperties;
  private VoiceProperties voiceProperties;
  private ProdStartupValidator validator;

  @BeforeEach
  void setUp() {
    mockEnvironment = new MockEnvironment();
    mockEnvironment.setActiveProfiles("prod");
    myDataProperties = new MyDataProperties();
    socialLoginProperties = new SocialLoginProperties();
    externalFinanceProperties = new ExternalFinanceProperties();
    apiProperties = new BurtyApiProperties();
    identityProperties = new IdentityProperties();
    notifyProperties = new NotifyProperties();
    securityProperties = new BurtySecurityProperties();
    voiceProperties = new VoiceProperties();
    validator =
        new ProdStartupValidator(
            mockEnvironment,
            myDataProperties,
            socialLoginProperties,
            externalFinanceProperties,
            apiProperties,
            identityProperties,
            notifyProperties,
            securityProperties,
            voiceProperties);
    configureValidProd();
  }

  @Test
  void skipsValidationOutsideProdProfile() {
    mockEnvironment.setActiveProfiles("dev");
    validator.validate();
  }

  @Test
  void blocksMissingTrustedProxiesInProd() {
    // 비워두면 모든 요청이 프록시 IP 로 기록된다. 레이트리밋도 접근 기록도 의미를 잃는다.
    securityProperties.setTrustedProxies(java.util.List.of());
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.security.trusted-proxies"));
  }

  @Test
  void blocksIdentityStubModeInProd() {
    identityProperties.setStubMode(true);
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.identity.stub-mode"));
  }

  @Test
  void blocksNotifyEmailStubModeInProd() {
    notifyProperties.getEmail().setStubMode(true);
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.notify.email.stub-mode"));
  }

  @Test
  void blocksDefaultFieldEncryptionKeyInProd() {
    mockEnvironment.setProperty(
        "burty.security.field-encryption-key", "change-me-burty-field-encryption-key-32");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("field-encryption-key"));
  }

  @Test
  void blocksDefaultWebAuthnSecretInProd() {
    mockEnvironment.setProperty("burty.webauthn.server-secret", "change-me-webauthn-secret");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("webauthn.server-secret"));
  }

  @Test
  void blocksEmptyWebAuthnSecretInProd() {
    // 빈 값이면 서명이 성립하지 않아 인증이 통째로 무력화된다. 시크릿을 지우는 것이
    // 인증을 끄는 결과가 되므로, 기본값만 막아서는 부족하다.
    mockEnvironment.setProperty("burty.webauthn.server-secret", "");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("webauthn.server-secret"));
  }

  @Test
  void blocksMockConsentAuthorizeUrlInProd() {
    // 모의 동의 화면은 개발·시연용이다. 운영 사용자가 실제 기관이 아닌 화면에서 동의하면 안 된다.
    myDataProperties.setAuthorizeUrl("https://burty.co.kr/mydata/mock-consent");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("authorize-url"));
  }

  @Test
  void blocksDefaultMyDataClientSecretInProd() {
    // 기본값으로는 실제 정보제공자와 토큰을 교환할 수 없다. 앱은 뜨고, 사용자가 기관을
    // 연결하는 순간에야 실패한다 (#160).
    myDataProperties.setClientSecret("burty-secret");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.mydata.client-secret"));
  }

  @Test
  void blocksBlankMyDataClientSecretInProd() {
    myDataProperties.setClientSecret(" ");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.mydata.client-secret"));
  }

  @Test
  void blocksDefaultMyDataClientIdInProd() {
    myDataProperties.setClientId("burty-client");
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.mydata.client-id"));
  }

  @Test
  void passesWhenProdConfigurationIsValid() {
    validator.validate();
    assertEquals("prod", mockEnvironment.getActiveProfiles()[0]);
  }

  @Test
  void blocksVoiceEnabledWithPlaceholderEndpointInProd() {
    // 자리표시자 주소(api.voice.local)를 그대로 두고 스텁만 끄면 앱은 정상 기동하고,
    // 사용자가 마이크를 누르는 순간에야 실패한다 (#129).
    voiceProperties.setStubMode(false);
    IllegalStateException error =
        assertThrows(IllegalStateException.class, () -> validator.validate());
    assertTrue(error.getMessage().contains("burty.voice"));
  }

  @Test
  void allowsVoiceStubModeInProd() {
    // 음성은 선택 기능이다. 마이데이터·본인확인과 달리 켜지 않았다고 배포를 막으면
    // 제공자가 정해지기 전까지 아무것도 띄울 수 없다.
    voiceProperties.setStubMode(true);
    validator.validate();
  }

  @Test
  void allowsVoiceWithRealProviderInProd() {
    voiceProperties.setStubMode(false);
    voiceProperties.setSttUrl("https://stt.example.com/v1/recognize");
    voiceProperties.setTtsUrl("https://tts.example.com/v1/synthesize");
    voiceProperties.setApiKey("real-key");
    validator.validate();
  }

  private void configureValidProd() {
    securityProperties.setTrustedProxies(java.util.List.of("10.0.0.0/8"));
    myDataProperties.setStubMode(false);
    myDataProperties.setClientId("prod-mydata-client");
    myDataProperties.setClientSecret("prod-mydata-client-secret");
    socialLoginProperties.setStubMode(false);
    externalFinanceProperties.setStubMode(false);
    apiProperties.setSwaggerEnabled(false);
    identityProperties.setStubMode(false);
    identityProperties.setProvider(IdentityProperties.Provider.NICE);
    identityProperties.getNice().setSiteCode("site");
    identityProperties.getNice().setSitePassword("password");
    notifyProperties.getEmail().setStubMode(false);
    notifyProperties.getSms().setStubMode(false);
    notifyProperties.getPush().setStubMode(false);
    notifyProperties.getSms().setApiKey("api-key");
    notifyProperties.getSms().setApiSecret("api-secret");
    notifyProperties.getSms().setSenderNumber("01012345678");
    notifyProperties.getPush().setFcmProjectId("burty-prod");
    notifyProperties.getPush().setFcmCredentialsJson("{\"project_id\":\"burty-prod\"}");
    mockEnvironment.setProperty("burty.jwt.secret", "prod-jwt-secret-with-sufficient-length-value");
    mockEnvironment.setProperty("burty.sign.secret", "prod-sign-secret-value");
    mockEnvironment.setProperty("burty.admin.setup-key", "prod-admin-setup-key");
    mockEnvironment.setProperty(
        "burty.security.field-encryption-key", "prod-field-encryption-key-32bytes!");
    mockEnvironment.setProperty("burty.webauthn.server-secret", "prod-webauthn-secret-value");
    mockEnvironment.setProperty("burty.redis.enabled", "true");
    mockEnvironment.setProperty("spring.mail.host", "smtp.example.com");
  }
}
