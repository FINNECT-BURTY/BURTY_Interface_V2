package com.burty.core.jenkins.exception;

/**
 * Jenkins Build 실행 중 오류 발생 시 발생하는 예외
 */
public class JenkinsBuildException extends JenkinsException {

    public JenkinsBuildException(String message) {
        super(message);
    }

    public JenkinsBuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
