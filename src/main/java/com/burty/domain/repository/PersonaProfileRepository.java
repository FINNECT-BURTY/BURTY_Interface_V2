package com.burty.domain.repository;

import com.burty.domain.entity.PersonaProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonaProfileRepository extends JpaRepository<PersonaProfileEntity, Long> {
    Optional<PersonaProfileEntity> findByUserId(Long userId);
}
