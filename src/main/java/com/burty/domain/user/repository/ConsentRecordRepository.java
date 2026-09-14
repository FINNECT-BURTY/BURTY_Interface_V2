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

  /**
   * 살아 있는 동의가 있는가.
   *
   * <p>철회한 동의는 없는 것으로 본다. 개인정보를 외부로 보내기 전에 이 값을 확인한다 — 동의 화면에서 체크했는지가 아니라 <b>기록이 남아 있는지</b>가 기준이다.
   */
  boolean existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
      Long userId, ConsentRecordEntity.ConsentType consentType);
}
