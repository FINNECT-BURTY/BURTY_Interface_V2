/**
 *
 *
 * <pre>
 * <b>Description  : 관리 응답 DTO (BaseCodeItemResponse)</b>
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

import com.burty.domain.admin.entity.BaseCodeEntity;

public record BaseCodeItemResponse(
    String codeId,
    String codeGroup,
    String codeValue,
    String codeNameKo,
    String codeNameEn,
    String parentCodeId,
    Integer sortOrder,
    String useYn,
    String description,
    String attr1,
    String attr2,
    String attr3,
    String attr4,
    String attr5) {

  public static BaseCodeItemResponse from(BaseCodeEntity entity) {
    return new BaseCodeItemResponse(
        entity.getCodeId(),
        entity.getCodeGroup(),
        entity.getCodeValue(),
        entity.getCodeNameKo(),
        entity.getCodeNameEn(),
        entity.getParentCodeId(),
        entity.getSortOrder(),
        entity.getUseYn(),
        entity.getDescription(),
        entity.getAttr1(),
        entity.getAttr2(),
        entity.getAttr3(),
        entity.getAttr4(),
        entity.getAttr5());
  }
}
