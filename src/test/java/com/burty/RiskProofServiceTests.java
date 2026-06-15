/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (RiskProofServiceTests)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
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
package com.burty;

import com.burty.config.JwtProperties;
import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RiskProofServiceTests {

  @Test
  void issuedProof_isVerifiableForSameUserAndLevel() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("test-secret-1234567890");
    RiskProofService service = new RiskProofService(properties);

    String token = service.issue("user-a", RiskLevel.LEVEL_2);

    Assertions.assertTrue(service.verify(token, "user-a", RiskLevel.LEVEL_2));
    Assertions.assertFalse(service.verify(token, "user-b", RiskLevel.LEVEL_2));
    Assertions.assertFalse(service.verify(token, "user-a", RiskLevel.LEVEL_3));
  }
}
