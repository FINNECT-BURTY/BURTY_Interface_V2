/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 응답 DTO (DeviceResponse)</b>
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

public record DeviceResponse(
    String deviceId,
    String deviceName,
    String platform,
    String osVersion,
    String appVersion,
    boolean trusted,
    LocalDateTime lastSeenAt,
    LocalDateTime createdAt) {}
