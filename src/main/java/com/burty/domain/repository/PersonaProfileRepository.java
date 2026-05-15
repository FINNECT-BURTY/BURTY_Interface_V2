package com.burty.domain.repository;

import com.burty.domain.entity.PersonaProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonaProfileRepository extends JpaRepository<PersonaProfileEntity, UUID> {
    Optional<PersonaProfileEntity> findByUserId(UUID userId);
}
