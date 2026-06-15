/**
 *
 *
 * <pre>
 * <b>Description  : 보안 포트 인터페이스 (BiometricAuthPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.security
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
package com.burty.application.port.out.security;

public interface BiometricAuthPort {
  boolean verifyAssertion(String userId, String assertionToken);
}
