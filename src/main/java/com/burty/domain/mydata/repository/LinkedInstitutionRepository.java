/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 리포지토리 (LinkedInstitutionRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.mydata.repository
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
package com.burty.domain.mydata.repository;

import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedInstitutionRepository extends JpaRepository<LinkedInstitutionEntity, Long> {
  List<LinkedInstitutionEntity> findByUser_UserId(Long userId);

  Optional<LinkedInstitutionEntity> findByUser_UserIdAndInstitutionCode(
      Long userId, String institutionCode);

  /**
   * 키 로테이션 배치용 — PK 순서로 한 페이지씩 훑는다.
   *
   * <p>전체를 한 번에 올리지 않는다. 암호문은 행마다 들고 있어야 하므로 건수가 많아지면 메모리를 그대로 먹는다. 마지막으로 처리한 PK 이후만 가져와 재시작에도
   * 안전하다.
   */
  List<LinkedInstitutionEntity> findByLinkIdGreaterThanOrderByLinkIdAsc(
      Long afterLinkId, Limit limit);
}
