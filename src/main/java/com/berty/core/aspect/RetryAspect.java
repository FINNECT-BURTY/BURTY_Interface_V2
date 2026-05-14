package com.berty.core.aspect;

import com.berty.core.annotation.Retryable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RetryAspect {

    /**
     * @Retryable 어노테이션이 적용된 메서드의 재시도를 처리합니다.
     */
    @Around("@annotation(com.berty.core.annotation.Retryable)")
    public Object retry(ProceedingJoinPoint joinPoint) throws Throwable {
        Retryable annotation = getAnnotation(joinPoint, Retryable.class);

        int maxAttempts = annotation.maxAttempts();
        long delay = annotation.delay();
        boolean exponentialBackoff = annotation.exponentialBackoff();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Exception e) {
                lastException = e;

                // 재시도하지 않을 예외인지 확인
                if (shouldNotRetry(e, annotation)) {
                    throw e;
                }

                // 마지막 시도인 경우 예외를 그대로 던짐
                if (attempt == maxAttempts) {
                    log.error("All {} attempts failed for method {}", maxAttempts,
                            joinPoint.getSignature().toShortString(), e);
                    throw e;
                }

                // 재시도 조건 확인
                if (!shouldRetry(joinPoint, e, annotation)) {
                    throw e;
                }

                log.warn("Attempt {} failed for method {}, retrying in {}ms",
                        attempt, joinPoint.getSignature().toShortString(), delay);

                // 지연 시간 계산
                long currentDelay = exponentialBackoff ? delay * (1L << (attempt - 1)) : delay;

                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw lastException;
    }

    /**
     * 재시도하지 않을 예외인지 확인합니다.
     */
    private boolean shouldNotRetry(Exception e, Retryable annotation) {
        Class<? extends Throwable>[] noRetryFor = annotation.noRetryFor();

        for (Class<? extends Throwable> exceptionClass : noRetryFor) {
            if (exceptionClass.isAssignableFrom(e.getClass())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 재시도할지 여부를 확인합니다.
     */
    private boolean shouldRetry(ProceedingJoinPoint joinPoint, Exception e, Retryable annotation) {
        Class<? extends Throwable>[] retryFor = annotation.retryFor();

        // 기본적으로 모든 예외에 대해 재시도
        if (retryFor.length == 0) {
            return true;
        }

        // 지정된 예외 타입에 대해서만 재시도
        for (Class<? extends Throwable> exceptionClass : retryFor) {
            if (exceptionClass.isAssignableFrom(e.getClass())) {
                return true;
            }
        }

        return false;
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