/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 응답 DTO (InstitutionResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.mydata
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
package com.burty.application.dto.mydata;

import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import java.time.LocalDateTime;

public record InstitutionResponse(
    String institutionCode,
    String status,
    LocalDateTime linkedAt,
    LocalDateTime tokenExpiresAt,
    LocalDateTime unlinkedAt,
    String lastErrorCode,
    LocalDateTime lastErrorAt) {

  public static InstitutionResponse from(MyDataLinkStatusEntity entity) {
    return new InstitutionResponse(
        entity.getInstitutionCode(),
        entity.getStatus(),
        entity.getLinkedAt(),
        entity.getTokenExpiresAt(),
        entity.getUnlinkedAt(),
        entity.getLastErrorCode(),
        entity.getLastErrorAt());
  }
}
