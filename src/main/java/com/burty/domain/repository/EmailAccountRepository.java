package com.burty.domain.repository;

import com.burty.domain.entity.EmailAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailAccountRepository extends JpaRepository<EmailAccountEntity, Long> {

    Optional<EmailAccountEntity> findByEmailHash(String emailHash);

    boolean existsByEmailHash(String emailHash);
}
