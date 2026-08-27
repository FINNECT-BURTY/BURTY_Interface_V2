package com.burty.domain.mydata.repository;

import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyDataTransmissionRequestRepository
    extends JpaRepository<MyDataTransmissionRequestEntity, Long> {

  List<MyDataTransmissionRequestEntity> findByUserIdOrderByRequestedAtDesc(String userId);

  Optional<MyDataTransmissionRequestEntity>
      findFirstByUserIdAndInstitutionCodeAndStatusOrderByRequestedAtDesc(
          String userId, String institutionCode, Status status);

  /**
   * 유효기간이 지난 활성 동의.
   *
   * <p>{@code consent_expires_at} 컬럼은 원래도 있었지만 아무도 읽지 않았다. 한 번 동의하면 토큰이 영구히 살아 있는 상태였다.
   */
  @Query(
      "select r from MyDataTransmissionRequestEntity r"
          + " where r.status in :activeStatuses"
          + "   and r.consentExpiresAt is not null and r.consentExpiresAt <= :now"
          + " order by r.requestId asc")
  List<MyDataTransmissionRequestEntity> findExpirable(
      @Param("activeStatuses") java.util.Collection<Status> activeStatuses,
      @Param("now") LocalDateTime now);

  /** 유효기간이 지난 활성 동의. enum 은 파라미터로 바인딩한다. */
  default List<MyDataTransmissionRequestEntity> findExpirable(LocalDateTime now) {
    return findExpirable(List.of(Status.AUTHORIZED, Status.ACTIVE), now);
  }

  /** 곧 만료될 동의. 예고 없이 끊기면 사용자는 서비스 장애로 인식한다. */
  @Query(
      "select r from MyDataTransmissionRequestEntity r"
          + " where r.status in :activeStatuses"
          + "   and r.consentExpiresAt is not null"
          + "   and r.consentExpiresAt > :now and r.consentExpiresAt <= :until"
          + " order by r.consentExpiresAt asc")
  List<MyDataTransmissionRequestEntity> findExpiringSoon(
      @Param("activeStatuses") java.util.Collection<Status> activeStatuses,
      @Param("now") LocalDateTime now,
      @Param("until") LocalDateTime until);

  /** 곧 만료될 동의. enum 은 파라미터로 바인딩한다. */
  default List<MyDataTransmissionRequestEntity> findExpiringSoon(
      LocalDateTime now, LocalDateTime until) {
    return findExpiringSoon(List.of(Status.AUTHORIZED, Status.ACTIVE), now, until);
  }

  List<MyDataTransmissionRequestEntity> findByUserId(String userId);
}
