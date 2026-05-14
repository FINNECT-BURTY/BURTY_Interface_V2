package com.berty.core.jenkins.exception;

/**
 * Jenkins 관련 예외의 기본 클래스
 */
public class JenkinsException extends RuntimeException {

    public JenkinsException(String message) {
        super(message);
    }

    public JenkinsException(String message, Throwable cause) {
        super(message, cause);
    }
}
