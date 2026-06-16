/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 리포지토리 (MyDataLinkStatusRepository)</b>
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

import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataLinkStatusRepository extends JpaRepository<MyDataLinkStatusEntity, String> {
  List<MyDataLinkStatusEntity> findByUserId(String userId);

  Optional<MyDataLinkStatusEntity> findByUserIdAndInstitutionCode(
      String userId, String institutionCode);

  List<MyDataLinkStatusEntity> findByStatusAndTokenExpiresAtBefore(
      String status, LocalDateTime before);

  List<MyDataLinkStatusEntity> findByStatus(String status);
}
