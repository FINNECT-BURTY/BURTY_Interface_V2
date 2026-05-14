package com.berty.domain.repository;

import com.berty.domain.entity.MyDataLinkStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MyDataLinkStatusRepository extends JpaRepository<MyDataLinkStatusEntity, String> {
    List<MyDataLinkStatusEntity> findByUserId(String userId);

    Optional<MyDataLinkStatusEntity> findByUserIdAndInstitutionCode(String userId, String institutionCode);

    List<MyDataLinkStatusEntity> findByStatusAndTokenExpiresAtBefore(String status, LocalDateTime before);

    List<MyDataLinkStatusEntity> findByStatus(String status);
}
