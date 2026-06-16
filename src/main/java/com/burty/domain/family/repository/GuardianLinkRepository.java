/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 리포지토리 (GuardianLinkRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.family.repository
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
package com.burty.domain.family.repository;

import com.burty.domain.family.entity.GuardianLinkEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianLinkRepository extends JpaRepository<GuardianLinkEntity, Long> {
  List<GuardianLinkEntity> findBySeniorUser_UserId(Long seniorUserId);
}
