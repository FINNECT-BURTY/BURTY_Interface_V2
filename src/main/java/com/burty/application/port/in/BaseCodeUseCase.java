package com.burty.application.port.in;

import com.burty.domain.entity.BaseCodeEntity;

import java.util.List;
import java.util.Optional;

public interface BaseCodeUseCase {

    List<BaseCodeEntity> lookup(String codeGroup);

    Optional<BaseCodeEntity> lookup(String codeGroup, String codeValue);

    List<BaseCodeEntity> children(String parentCodeId);

    String displayName(String codeGroup, String codeValue, String localeTag);

    BaseCodeEntity upsert(BaseCodeEntity entity, String operator);

    void deactivate(String codeId, String operator);

    void reload();
}