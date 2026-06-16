/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 응답 DTO (FamilyAlertResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.family
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
package com.burty.application.dto.family;

import java.time.LocalDateTime;

public record FamilyAlertResponse(String userId, String message, LocalDateTime sentAt) {}
