/**
 *
 *
 * <pre>
 * <b>Description  : 관리 리포지토리 (BaseCodeRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.admin.repository
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
package com.burty.domain.admin.repository;

import com.burty.domain.admin.entity.BaseCodeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseCodeRepository extends JpaRepository<BaseCodeEntity, String> {

  List<BaseCodeEntity> findByCodeGroupAndUseYnOrderBySortOrderAsc(String codeGroup, String useYn);

  Optional<BaseCodeEntity> findByCodeGroupAndCodeValue(String codeGroup, String codeValue);

  List<BaseCodeEntity> findByParentCodeIdOrderBySortOrderAsc(String parentCodeId);
}
