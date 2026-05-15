package com.burty.domain.repository;

import com.burty.domain.entity.AiFallbackTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiFallbackTemplateRepository extends JpaRepository<AiFallbackTemplateEntity, String> {
    List<AiFallbackTemplateEntity> findByActiveTrue();
}
