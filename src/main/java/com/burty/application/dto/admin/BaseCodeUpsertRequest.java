/**
 *
 *
 * <pre>
 * <b>Description  : 관리 요청 DTO (BaseCodeUpsertRequest)</b>
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

public record BaseCodeUpsertRequest(
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

  public BaseCodeEntity toEntity() {
    BaseCodeEntity entity = new BaseCodeEntity();
    entity.setCodeId(codeId);
    entity.setCodeGroup(codeGroup);
    entity.setCodeValue(codeValue);
    entity.setCodeNameKo(codeNameKo);
    entity.setCodeNameEn(codeNameEn);
    entity.setParentCodeId(parentCodeId);
    entity.setSortOrder(sortOrder);
    entity.setUseYn(useYn);
    entity.setDescription(description);
    entity.setAttr1(attr1);
    entity.setAttr2(attr2);
    entity.setAttr3(attr3);
    entity.setAttr4(attr4);
    entity.setAttr5(attr5);
    return entity;
  }
}
