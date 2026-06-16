/**
 *
 *
 * <pre>
 * <b>Description  : 인증 도메인 모델 (BiometricAuthResult)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.model
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
package com.burty.domain.auth.model;

public record BiometricAuthResult(
    String userId,
    String deviceId,
    String deviceToken,
    String accessToken,
    boolean authenticated,
    boolean trustedDevice) {}
