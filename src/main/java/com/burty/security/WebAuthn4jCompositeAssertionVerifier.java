/**
 *
 *
 * <pre>
 * <b>Description  : 보안 (WebAuthn4jCompositeAssertionVerifier)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.security
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.security;

import com.webauthn4j.WebAuthnAuthenticationManager;
import com.webauthn4j.WebAuthnRegistrationManager;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** WebAuthn4J로 등록/인증을 검증하고, 클라이언트 페이로드가 표준 형식이 아니면 {@link StandardLikeWebAuthnVerifier}로 폴백합니다. */
public class WebAuthn4jCompositeAssertionVerifier implements WebAuthnAssertionVerifier {

  private final StandardLikeWebAuthnVerifier fallback;
  private final ObjectConverter objectConverter = new ObjectConverter();
  private final WebAuthnRegistrationManager registrationManager =
      WebAuthnRegistrationManager.createNonStrictWebAuthnRegistrationManager(objectConverter);
  private final WebAuthnAuthenticationManager authenticationManager =
      new WebAuthnAuthenticationManager(java.util.List.of(), objectConverter);

  public WebAuthn4jCompositeAssertionVerifier(StandardLikeWebAuthnVerifier fallback) {
    this.fallback = fallback;
  }

  @Override
  public VerificationResult verifyRegistration(
      String payload,
      String expectedChallenge,
      String expectedOrigin,
      String expectedRpId,
      long currentSignCount) {
    try {
      ServerProperty serverProperty =
          serverProperty(expectedChallenge, expectedOrigin, expectedRpId);
      RegistrationData data =
          registrationManager.verify(payload, new RegistrationParameters(serverProperty, false));
      var attested = data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
      if (attested == null) {
        return fallback.verifyRegistration(
            payload, expectedChallenge, expectedOrigin, expectedRpId, currentSignCount);
      }
      byte[] credId = attested.getCredentialId();
      COSEKey coseKey = attested.getCOSEKey();
      byte[] coseBytes = objectConverter.getCborConverter().writeValueAsBytes(coseKey);
      String credB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(credId);
      String keyB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(coseBytes);
      long authenticatorCounter = data.getAttestationObject().getAuthenticatorData().getSignCount();
      long next = Math.max(authenticatorCounter, currentSignCount + 1);
      return new VerificationResult(true, next, credB64, keyB64);
    } catch (Exception ignored) {
      return fallback.verifyRegistration(
          payload, expectedChallenge, expectedOrigin, expectedRpId, currentSignCount);
    }
  }

  @Override
  public VerificationResult verifyAuthentication(
      String payload,
      String expectedChallenge,
      String expectedOrigin,
      String expectedRpId,
      long currentSignCount,
      WebAuthnStoredCredential storedCredential) {
    if (storedCredential == null || storedCredential.getCosePublicKey().length == 0) {
      return fallback.verifyAuthentication(
          payload,
          expectedChallenge,
          expectedOrigin,
          expectedRpId,
          currentSignCount,
          storedCredential);
    }
    COSEKey coseKey;
    try {
      coseKey =
          objectConverter
              .getCborConverter()
              .readValue(storedCredential.getCosePublicKey(), COSEKey.class);
    } catch (Exception ignored) {
      return fallback.verifyAuthentication(
          payload,
          expectedChallenge,
          expectedOrigin,
          expectedRpId,
          currentSignCount,
          storedCredential);
    }
    try {
      ServerProperty serverProperty =
          serverProperty(expectedChallenge, expectedOrigin, expectedRpId);
      AttestedCredentialData acd =
          new AttestedCredentialData(AAGUID.ZERO, storedCredential.getCredentialIdRaw(), coseKey);
      AuthenticatorImpl authenticator =
          new AuthenticatorImpl(
              acd, new NoneAttestationStatement(), storedCredential.getSignCount());
      AuthenticationParameters parameters =
          new AuthenticationParameters(serverProperty, authenticator, false);
      AuthenticationData authData = authenticationManager.verify(payload, parameters);
      long next = authData.getAuthenticatorData().getSignCount();
      if (next <= storedCredential.getSignCount()) {
        return new VerificationResult(false, storedCredential.getSignCount(), "", "");
      }
      byte[] cid = authData.getCredentialId();
      String credB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(cid);
      String keyB64 =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(storedCredential.getCosePublicKey());
      return new VerificationResult(true, next, credB64, keyB64);
    } catch (Exception ignored) {
      return fallback.verifyAuthentication(
          payload,
          expectedChallenge,
          expectedOrigin,
          expectedRpId,
          currentSignCount,
          storedCredential);
    }
  }

  private ServerProperty serverProperty(
      String expectedChallenge, String expectedOrigin, String expectedRpId) {
    var challenge = new DefaultChallenge(expectedChallenge.getBytes(StandardCharsets.UTF_8));
    return new ServerProperty(new Origin(expectedOrigin), expectedRpId, challenge);
  }
}
