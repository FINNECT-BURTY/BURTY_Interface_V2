/**
 *
 *
 * <pre>
 * <b>Description  : 관리 도메인 모델 (AuditEvent)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.admin.model
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
package com.burty.domain.admin.model;

import java.time.LocalDateTime;

public record AuditEvent(
    String traceId,
    String actorId,
    String action,
    String target,
    String result,
    String detail,
    LocalDateTime createdAt) {}
