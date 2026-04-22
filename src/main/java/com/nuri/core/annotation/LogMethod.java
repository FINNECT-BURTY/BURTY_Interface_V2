package com.nuri.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행을 로깅하기 위한 어노테이션
 * AOP를 통해 메서드 실행 전후로 로그를 출력합니다.
 *
 * @author EduVerse
 * @version 1.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethod {

    /**
     * 로그 레벨
     * DEBUG, INFO, WARN, ERROR 중 선택
     */
    String level() default "INFO";

    /**
     * 메서드 시작 로그 포함 여부
     * true: 메서드 시작 시 로그 출력
     * false: 메서드 시작 로그 출력 안함
     */
    boolean logStart() default true;

    /**
     * 메서드 종료 로그 포함 여부
     * true: 메서드 종료 시 로그 출력
     * false: 메서드 종료 로그 출력 안함
     */
    boolean logEnd() default true;

    /**
     * 파라미터 로그 포함 여부
     * true: 메서드 파라미터 로그 출력
     * false: 메서드 파라미터 로그 출력 안함
     */
    boolean logParameters() default false;

    /**
     * 반환값 로그 포함 여부
     * true: 메서드 반환값 로그 출력
     * false: 메서드 반환값 로그 출력 안함
     */
    boolean logReturnValue() default false;

    /**
     * 예외 로그 포함 여부
     * true: 예외 발생 시 로그 출력
     * false: 예외 로그 출력 안함
     */
    boolean logException() default true;

    /**
     * 실행 시간 로그 포함 여부
     * true: 메서드 실행 시간 로그 출력
     * false: 실행 시간 로그 출력 안함
     */
    boolean logExecutionTime() default false;
}
