package com.berty.domain.repository;

import com.berty.domain.entity.AiFallbackTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiFallbackTemplateRepository extends JpaRepository<AiFallbackTemplateEntity, String> {
    List<AiFallbackTemplateEntity> findByActiveTrue();
}
