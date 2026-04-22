package com.nuri.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시간을 측정하고 로깅하기 위한 어노테이션
 * AOP를 통해 메서드 실행 시간을 자동으로 측정하고 로그를 출력합니다.
 *
 * @author EduVerse
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {

    /**
     * 로그 레벨
     * DEBUG, INFO, WARN, ERROR 중 선택
     */
    String level() default "INFO";

    /**
     * 메서드명 포함 여부
     * true: 로그에 메서드명 포함
     * false: 로그에 메서드명 제외
     */
    boolean includeMethodName() default true;

    /**
     * 클래스명 포함 여부
     * true: 로그에 클래스명 포함
     * false: 로그에 클래스명 제외
     */
    boolean includeClassName() default true;

    /**
     * 파라미터 정보 포함 여부
     * true: 로그에 메서드 파라미터 정보 포함
     * false: 로그에 메서드 파라미터 정보 제외
     */
    boolean includeParameters() default false;
}