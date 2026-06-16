/**
 *
 *
 * <pre>
 * <b>Description  : 코어 예외 (JenkinsException)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.exception
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
package com.burty.core.jenkins.exception;

/** Jenkins 관련 예외의 기본 클래스 */
public class JenkinsException extends RuntimeException {

  public JenkinsException(String message) {
    super(message);
  }

  public JenkinsException(String message, Throwable cause) {
    super(message, cause);
  }
}
