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

import com.burty.security.StandardLikeWebAuthnVerifier;
import com.burty.security.WebAuthn4jCompositeAssertionVerifier;
import com.burty.security.WebAuthnAssertionVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WebAuthnVerifierConfiguration {

  @Bean
  public StandardLikeWebAuthnVerifier standardLikeWebAuthnVerifier() {
    return new StandardLikeWebAuthnVerifier();
  }

  @Bean
  @Primary
  public WebAuthnAssertionVerifier webAuthnAssertionVerifier(
      StandardLikeWebAuthnVerifier standardLikeWebAuthnVerifier) {
    return new WebAuthn4jCompositeAssertionVerifier(standardLikeWebAuthnVerifier);
  }
}
