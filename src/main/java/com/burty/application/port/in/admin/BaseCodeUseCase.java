/**
 *
 *
 * <pre>
 * <b>Description  : 관리 유스케이스 포트 (BaseCodeUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.admin
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
package com.burty.application.port.in.admin;

import com.burty.domain.admin.entity.BaseCodeEntity;
import java.util.List;
import java.util.Optional;

public interface BaseCodeUseCase {

  List<BaseCodeEntity> lookup(String codeGroup);

  Optional<BaseCodeEntity> lookup(String codeGroup, String codeValue);

  List<BaseCodeEntity> children(String parentCodeId);

  String displayName(String codeGroup, String codeValue, String localeTag);

  BaseCodeEntity upsert(BaseCodeEntity entity, String operator);

  void deactivate(String codeId, String operator);

  void reload();
}
