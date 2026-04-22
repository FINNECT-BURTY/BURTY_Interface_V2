package com.nuri.core.jenkins.exception;

/**
 * Jenkins 서버 연결 실패 시 발생하는 예외
 */
public class JenkinsConnectionException extends JenkinsException {

    public JenkinsConnectionException(String message) {
        super(message);
    }

    public JenkinsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
