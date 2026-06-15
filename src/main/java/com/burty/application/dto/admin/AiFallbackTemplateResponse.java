/**
 *
 *
 * <pre>
 * <b>Description  : 관리 응답 DTO (AiFallbackTemplateResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.admin
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
package com.burty.application.dto.admin;

import com.burty.domain.admin.entity.AiFallbackTemplateEntity;
import java.time.LocalDateTime;

public record AiFallbackTemplateResponse(
    String templateKey,
    String riskLevel,
    String occupationCode,
    String causeType,
    String templateText,
    boolean active,
    LocalDateTime updatedAt) {

  public static AiFallbackTemplateResponse from(AiFallbackTemplateEntity entity) {
    return new AiFallbackTemplateResponse(
        entity.getTemplateKey(),
        entity.getRiskLevel(),
        entity.getOccupationCode(),
        entity.getCauseType(),
        entity.getTemplateText(),
        Boolean.TRUE.equals(entity.getActive()),
        entity.getUpdatedAt());
  }
}
