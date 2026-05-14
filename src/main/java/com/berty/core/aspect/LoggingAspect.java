package com.berty.core.aspect;

import com.berty.core.annotation.LogExecutionTime;
import com.berty.core.annotation.LogMethod;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * @LogExecutionTime 어노테이션이 적용된 메서드의 실행 시간을 측정합니다.
     */
    @Around("@annotation(com.berty.core.annotation.LogExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        LogExecutionTime annotation = getAnnotation(joinPoint, LogExecutionTime.class);

        long startTime = System.currentTimeMillis();
        String methodName = getMethodName(joinPoint);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logMethodExecution(methodName, executionTime, annotation, null);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodExecution(methodName, executionTime, annotation, e);
            throw e;
        }
    }

    /**
     * @LogMethod 어노테이션이 적용된 메서드의 실행을 로깅합니다.
     */
    @Around("@annotation(com.berty.core.annotation.LogMethod)")
    public Object logMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        LogMethod annotation = getAnnotation(joinPoint, LogMethod.class);
        String methodName = getMethodName(joinPoint);

        if (annotation.logStart()) {
            logMethodStart(methodName, joinPoint, annotation);
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            if (annotation.logEnd()) {
                logMethodEnd(methodName, result, executionTime, annotation);
            }

            if (annotation.logException() && exception != null) {
                logMethodException(methodName, exception, annotation);
            }
        }
    }

    /**
     * 메서드 실행 로그를 출력합니다.
     */
    private void logMethodExecution(String methodName, long executionTime, LogExecutionTime annotation, Exception exception) {
        String level = annotation.level().toUpperCase();
        String message = String.format("[%s] %s executed in %dms", methodName, methodName, executionTime);

        switch (level) {
            case "DEBUG" -> log.debug(message);
            case "INFO" -> log.info(message);
            case "WARN" -> log.warn(message);
            case "ERROR" -> log.error(message);
            default -> log.info(message);
        }

        if (exception != null) {
            log.error("Exception in {}: {}", methodName, exception.getMessage(), exception);
        }
    }

    /**
     * 메서드 시작 로그를 출력합니다.
     */
    private void logMethodStart(String methodName, ProceedingJoinPoint joinPoint, LogMethod annotation) {
        StringBuilder message = new StringBuilder();
        message.append("Starting ").append(methodName);

        if (annotation.logParameters()) {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0) {
                message.append(" with parameters: ").append(Arrays.toString(args));
            }
        }

        log.info(message.toString());
    }

    /**
     * 메서드 종료 로그를 출력합니다.
     */
    private void logMethodEnd(String methodName, Object result, long executionTime, LogMethod annotation) {
        StringBuilder message = new StringBuilder();
        message.append("Completed ").append(methodName);

        if (annotation.logReturnValue() && result != null) {
            message.append(" with result: ").append(result);
        }

        if (annotation.logExecutionTime()) {
            message.append(" in ").append(executionTime).append("ms");
        }

        log.info(message.toString());
    }

    /**
     * 메서드 예외 로그를 출력합니다.
     */
    private void logMethodException(String methodName, Exception exception, LogMethod annotation) {
        log.error("Exception in {}: {}", methodName, exception.getMessage(), exception);
    }

    /**
     * 메서드명을 가져옵니다.
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        return className + "." + methodName;
    }

    /**
     * 어노테이션을 가져옵니다.
     */
    @SuppressWarnings("unchecked")
    private <T> T getAnnotation(ProceedingJoinPoint joinPoint, Class<T> annotationClass) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return (T) method.getAnnotation((Class<? extends java.lang.annotation.Annotation>) annotationClass);
    }
}