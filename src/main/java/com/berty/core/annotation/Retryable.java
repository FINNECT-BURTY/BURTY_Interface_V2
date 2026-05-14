package com.berty.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 실패 시 재시도하기 위한 어노테이션
 * AOP를 통해 메서드 실행이 실패하면 지정된 횟수만큼 재시도합니다.
 *
 * @author EduVerse
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {

    /**
     * 최대 재시도 횟수
     * 기본값: 3회
     */
    int maxAttempts() default 3;

    /**
     * 재시도 간격 (밀리초)
     * 기본값: 1000ms (1초)
     */
    long delay() default 1000;

    /**
     * 재시도 간격 배수
     * true: 재시도할 때마다 간격이 배수로 증가 (1초, 2초, 4초...)
     * false: 고정 간격으로 재시도
     */
    boolean exponentialBackoff() default false;

    /**
     * 재시도할 예외 클래스
     * 이 예외들이 발생하면 재시도
     * 기본값: 모든 예외
     */
    Class<? extends Throwable>[] retryFor() default {Exception.class};

    /**
     * 재시도하지 않을 예외 클래스
     * 이 예외들이 발생하면 재시도하지 않음
     */
    Class<? extends Throwable>[] noRetryFor() default {};

    /**
     * 재시도 조건
     * SpEL 표현식으로 재시도할지 여부를 결정
     * 기본값: 항상 재시도
     */
    String condition() default "";
}
