package com.berty.core.jenkins.exception;

// Jenkins Job을 찾을 수 없을 때 발생하는 예외
public class JenkinsJobNotFoundException extends JenkinsException {

    public JenkinsJobNotFoundException(String jobName) {
        super("Jenkins job not found: " + jobName);
    }

    public JenkinsJobNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
