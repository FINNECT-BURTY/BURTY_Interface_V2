package com.nuri.core.config;

import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR, // 매핑 된게 없으면 컴파일 에러로 조기 발견
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE // 부분 업데이트 할 때 사용
)
public interface GlobalMapperConfig {
}
