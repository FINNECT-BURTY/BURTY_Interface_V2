package com.berty.domain.repository;

import com.berty.domain.entity.PersonaProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonaProfileRepository extends JpaRepository<PersonaProfileEntity, UUID> {
    Optional<PersonaProfileEntity> findByUserId(UUID userId);
}
