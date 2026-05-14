package com.berty.domain.repository;

import com.berty.domain.entity.RegisteredAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegisteredAccountRepository extends JpaRepository<RegisteredAccountEntity, String> {
    List<RegisteredAccountEntity> findByUserId(String userId);

    boolean existsByUserIdAndAccountNoHash(String userId, String accountNoHash);
}
