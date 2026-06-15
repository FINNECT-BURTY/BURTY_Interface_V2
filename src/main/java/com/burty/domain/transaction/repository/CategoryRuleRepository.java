/**
 *
 *
 * <pre>
 * <b>Description  : 거래 리포지토리 (CategoryRuleRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.transaction.repository
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
package com.burty.domain.transaction.repository;

import com.burty.domain.transaction.entity.CategoryRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRuleRepository extends JpaRepository<CategoryRuleEntity, String> {
  List<CategoryRuleEntity> findByActiveTrueOrderByPriorityDesc();
}
