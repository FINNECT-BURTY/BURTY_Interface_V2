/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 리포지토리 (ConsentRecordRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.user.repository
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
package com.burty.domain.user.repository;

import com.burty.domain.user.entity.ConsentRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecordEntity, Long> {
  List<ConsentRecordEntity> findByUser_UserId(Long userId);

  List<ConsentRecordEntity> findByUser_UserIdOrderByAgreedAtDesc(Long userId);
}
