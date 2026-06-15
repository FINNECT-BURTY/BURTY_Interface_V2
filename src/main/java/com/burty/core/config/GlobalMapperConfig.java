/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (GlobalMapperConfig)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.core.config;

import org.mapstruct.*;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE // 부분 업데이트 할 때 사용
    )
public interface GlobalMapperConfig {}
