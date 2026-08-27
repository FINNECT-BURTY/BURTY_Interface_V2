package com.burty.domain.family.repository;

import com.burty.domain.family.entity.TransferApprovalEntity;
import com.burty.domain.family.entity.TransferApprovalEntity.Status;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferApprovalRepository extends JpaRepository<TransferApprovalEntity, Long> {

  Optional<TransferApprovalEntity> findByApprovalIdAndGuardianUserId(
      Long approvalId, String guardianUserId);

  List<TransferApprovalEntity> findByGuardianUserIdAndStatusOrderByApprovalIdDesc(
      String guardianUserId, Status status);

  List<TransferApprovalEntity> findByRequesterUserIdOrderByApprovalIdDesc(String requesterUserId);

  Optional<TransferApprovalEntity> findFirstByOrderIdOrderByApprovalIdDesc(Long orderId);

  /** 기한이 지난 미결 승인. 무한정 보류되면 사용자가 이체를 못 쓴다. */
  List<TransferApprovalEntity> findByStatusAndExpiresAtLessThanEqual(
      Status status, LocalDateTime now);
}
