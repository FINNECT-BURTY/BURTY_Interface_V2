package com.berty.core.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

public interface BaseMapper<E, D, R> {

    D toDto(E entity);
    E toEntity(D dto);

    R toResponse(E entity);

    // 단일 매핑이 있을 경우 MapStruct가 자동으로 List 매핑을 생성
    List<D> toDto(List<E> entities);
    List<E> toEntity(List<D> dtos);
    List<R> toResponse(List<E> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(D dto, @MappingTarget E entity);

    // Page 매핑 공통 처리
    default Page<D> toDto(Page<E> page) {
        return new PageImpl<D>(toDto(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    default Page<R> toResponse(Page<E> page) {
        return new PageImpl<>(toResponse(page.getContent()), page.getPageable(), page.getTotalElements());
    }

}
