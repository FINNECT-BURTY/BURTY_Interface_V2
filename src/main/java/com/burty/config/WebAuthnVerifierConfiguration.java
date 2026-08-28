/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (WebAuthnVerifierConfiguration)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import com.burty.security.StubWebAuthnVerifier;
import com.burty.security.WebAuthn4jCompositeAssertionVerifier;
import com.burty.security.WebAuthnAssertionVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WebAuthnVerifierConfiguration {

  /**
   * WebAuthn 어설션 검증기.
   *
   * <p>기본은 서명을 실제로 검증하는 WebAuthn4J 구현이다. 스텁은 <b>명시적으로 켰을 때만</b> 쓴다 — 예전에는 스텁이 검증 실패 시의 폴백으로 항상 붙어
   * 있어, 위조 페이로드가 이체의 생체인증 게이트를 통과했다.
   */
  @Bean
  @Primary
  public WebAuthnAssertionVerifier webAuthnAssertionVerifier(WebAuthnProperties properties) {
    if (properties.isStubMode()) {
      return new StubWebAuthnVerifier();
    }
    return new WebAuthn4jCompositeAssertionVerifier();
  }
}
