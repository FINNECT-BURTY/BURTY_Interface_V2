/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 응답 DTO (ConsentResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.user
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
package com.burty.application.dto.user;

import java.time.LocalDateTime;

public record ConsentResponse(
    String consentId,
    String consentType,
    String consentVersion,
    LocalDateTime agreedAt,
    LocalDateTime revokedAt) {}
