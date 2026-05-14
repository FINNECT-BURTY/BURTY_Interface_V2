package com.berty.domain.repository;

import com.berty.domain.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByLinkedInstitution_LinkId(UUID linkId);
}
