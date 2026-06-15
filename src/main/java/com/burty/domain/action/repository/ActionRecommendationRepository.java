/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 리포지토리 (ActionRecommendationRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.action.repository
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
package com.burty.domain.action.repository;

import com.burty.domain.action.entity.ActionRecommendationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionRecommendationRepository
    extends JpaRepository<ActionRecommendationEntity, String> {
  List<ActionRecommendationEntity> findByActiveTrue();
}
