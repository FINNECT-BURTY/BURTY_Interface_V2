package com.burty.domain.repository;

import com.burty.domain.entity.BaseCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaseCodeRepository extends JpaRepository<BaseCodeEntity, String> {

    List<BaseCodeEntity> findByCodeGroupAndUseYnOrderBySortOrderAsc(String codeGroup, String useYn);

    Optional<BaseCodeEntity> findByCodeGroupAndCodeValue(String codeGroup, String codeValue);

    List<BaseCodeEntity> findByParentCodeIdOrderBySortOrderAsc(String parentCodeId);
}