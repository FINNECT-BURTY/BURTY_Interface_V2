package com.burty.domain.repository;

import com.burty.domain.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findByLinkedInstitution_LinkId(Long linkId);
}
