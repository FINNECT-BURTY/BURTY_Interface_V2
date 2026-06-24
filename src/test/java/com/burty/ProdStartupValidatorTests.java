package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.config.BurtyApiProperties;
import com.burty.config.ExternalFinanceProperties;
import com.burty.config.IdentityProperties;
import com.burty.config.MyDataProperties;
import com.burty.config.NotifyProperties;
import com.burty.config.ProdStartupValidator;
import com.burty.config.SocialLoginProperties;
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
    validator =
        new ProdStartupValidator(
            mockEnvironment,
            myDataProperties,
            socialLoginProperties,
            externalFinanceProperties,
            apiProperties,
            identityProperties,
            notifyProperties);
    configureValidProd();
  }

  @Test
  void skipsValidationOutsideProdProfile() {
    mockEnvironment.setActiveProfiles("dev");
    validator.validate();
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
  void passesWhenProdConfigurationIsValid() {
    validator.validate();
    assertEquals("prod", mockEnvironment.getActiveProfiles()[0]);
  }

  private void configureValidProd() {
    myDataProperties.setStubMode(false);
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
    mockEnvironment.setProperty("burty.redis.enabled", "true");
    mockEnvironment.setProperty("spring.mail.host", "smtp.example.com");
  }
}
