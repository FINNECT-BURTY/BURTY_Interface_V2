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
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
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

/**
 * WebAuthn4J 로 등록·인증 어설션의 서명을 검증한다.
 *
 * <p><b>검증에 실패하면 실패다.</b> 예전에는 검증이 예외로 끝나면 문자열 일치만 보는 검증기로 폴백했다. 그 결과 기대 챌린지·오리진·rpId 와 {@code
 * "signature"} 키만 들어 있으면 서명이 없어도 통과했고, 이체의 생체인증 게이트가 그대로 뚫렸다. 폴백은 제거했다.
 *
 * <p>개발·테스트용 우회가 필요하면 {@code burty.webauthn.stub-mode=true} 로 {@link StubWebAuthnVerifier} 를 쓴다. 그
 * 설정은 prod 기동을 막는다.
 */
public class WebAuthn4jCompositeAssertionVerifier implements WebAuthnAssertionVerifier {

  private final ObjectConverter objectConverter = new ObjectConverter();
  private final WebAuthnRegistrationManager registrationManager =
      WebAuthnRegistrationManager.createNonStrictWebAuthnRegistrationManager(objectConverter);
  private final WebAuthnAuthenticationManager authenticationManager =
      new WebAuthnAuthenticationManager(java.util.List.of(), objectConverter);

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
          registrationManager.verify(
              payload, new RegistrationParameters(serverProperty, null, false));
      var attested = data.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
      if (attested == null) {
        return failed(currentSignCount);
      }
      byte[] credId = attested.getCredentialId();
      COSEKey coseKey = attested.getCOSEKey();
      byte[] coseBytes = objectConverter.getCborMapper().writeValueAsBytes(coseKey);
      String credB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(credId);
      String keyB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(coseBytes);
      long authenticatorCounter = data.getAttestationObject().getAuthenticatorData().getSignCount();
      long next = Math.max(authenticatorCounter, currentSignCount + 1);
      return new VerificationResult(true, next, credB64, keyB64);
    } catch (Exception ignored) {
      return failed(currentSignCount);
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
      return failed(currentSignCount);
    }
    COSEKey coseKey;
    try {
      coseKey =
          objectConverter
              .getCborMapper()
              .readValue(storedCredential.getCosePublicKey(), COSEKey.class);
    } catch (Exception ignored) {
      return failed(currentSignCount);
    }
    try {
      ServerProperty serverProperty =
          serverProperty(expectedChallenge, expectedOrigin, expectedRpId);
      AttestedCredentialData acd =
          new AttestedCredentialData(AAGUID.ZERO, storedCredential.getCredentialIdRaw(), coseKey);
      // uvInitialized / backupEligible / backupState 는 저장하지 않는다.
      // 이 검증기는 사용자 검증(UV)을 요구하지 않으므로 null 로 두어도 판정이 달라지지 않는다.
      CredentialRecordImpl credentialRecord =
          new CredentialRecordImpl(
              new NoneAttestationStatement(),
              null,
              null,
              null,
              storedCredential.getSignCount(),
              acd,
              null,
              null,
              null,
              null);
      AuthenticationParameters parameters =
          new AuthenticationParameters(serverProperty, credentialRecord, null, false);
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
      return failed(currentSignCount);
    }
  }

  /** 검증 실패. 사인 카운트는 올리지 않는다 — 올리면 정상 인증이 뒤이어 거부된다. */
  private static VerificationResult failed(long currentSignCount) {
    return new VerificationResult(false, currentSignCount, "", "");
  }

  private ServerProperty serverProperty(
      String expectedChallenge, String expectedOrigin, String expectedRpId) {
    var challenge = new DefaultChallenge(expectedChallenge.getBytes(StandardCharsets.UTF_8));
    return ServerProperty.builder()
        .origin(new Origin(expectedOrigin))
        .rpId(expectedRpId)
        .challenge(challenge)
        .build();
  }
}
