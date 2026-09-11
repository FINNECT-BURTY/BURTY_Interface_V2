package com.burty;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.adapter.out.security.WebAuthnCredentialStore;
import com.burty.adapter.out.security.WebAuthnDeviceTrustManager;
import com.burty.adapter.out.security.WebAuthnDeviceTrustManager.DeviceTokenPair;
import com.burty.adapter.out.security.WebAuthnFido2Adapter;
import com.burty.adapter.out.store.ChallengeStore;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.WebAuthnProperties;
import com.burty.domain.auth.model.BiometricAuthResult;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.security.JwtTokenProvider;
import com.burty.security.WebAuthnAssertionVerifier;
import com.burty.security.WebAuthnAssertionVerifier.VerificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 패스키 등록이 만드는 신뢰 기기.
 *
 * <p>예전에는 검증 단계가 {@code default-fingerprint} 기기를 먼저 만들고 자격증명을 거기에 묶은 뒤, 등록 단계가 실제 기기를 또 만들었다. 등록할
 * 때마다 쓰이지 않는 신뢰 기기가 하나씩 남아 기기 목록에 보이고, 실제 기기를 해제해도 신뢰 상태로 남았다 (#157).
 */
class WebAuthnRegistrationDeviceTests {

  private static final String USER_ID = "1";
  private static final String CHALLENGE_ID = "challenge-1";

  @Test
  @DisplayName("패스키를 한 번 등록하면 신뢰 기기는 실제 기기 하나이고, 자격증명은 그 기기에 묶인다")
  void registrationCreatesOnlyTheRealDevice() {
    ChallengeStore challengeStore = mock(ChallengeStore.class);
    when(challengeStore.get(CHALLENGE_ID)).thenReturn(USER_ID + "|REGISTRATION");
    when(challengeStore.consume(CHALLENGE_ID)).thenReturn(true);
    WebAuthnAssertionVerifier verifier = mock(WebAuthnAssertionVerifier.class);
    when(verifier.verifyRegistration(anyString(), anyString(), anyString(), anyString(), anyLong()))
        .thenReturn(new VerificationResult(true, 1L, "cred", "key"));
    WebAuthnCredentialStore credentialStore = mock(WebAuthnCredentialStore.class);
    WebAuthnDeviceTrustManager deviceTrust = mock(WebAuthnDeviceTrustManager.class);
    DeviceEntity realDevice = new DeviceEntity();
    realDevice.setDeviceId(10L);
    when(deviceTrust.ensureTrustedDevice(anyLong(), anyString(), anyString(), any()))
        .thenReturn(new DeviceTokenPair(realDevice, "bdt_real"));
    WebAuthnFido2Adapter adapter =
        new WebAuthnFido2Adapter(
            new WebAuthnProperties(),
            challengeStore,
            verifier,
            credentialStore,
            deviceTrust,
            mock(JwtTokenProvider.class),
            mock(AuditLogger.class));

    BiometricAuthResult result =
        adapter.registerTrustedDevice(
            USER_ID, CHALLENGE_ID, "signed-payload", "fp-real", "IOS", "FACE");

    assertTrue(result.authenticated());
    verify(deviceTrust, times(1)).ensureTrustedDevice(anyLong(), anyString(), anyString(), any());
    verify(deviceTrust).ensureTrustedDevice(eq(1L), eq("fp-real"), eq("IOS"), any());
    verify(credentialStore)
        .upsertFromRegistration(eq(1L), any(), argThat(pair -> pair.device() == realDevice));
  }
}
