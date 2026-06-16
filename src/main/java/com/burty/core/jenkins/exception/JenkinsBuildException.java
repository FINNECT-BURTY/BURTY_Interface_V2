/**
 *
 *
 * <pre>
 * <b>Description  : 코어 예외 (JenkinsBuildException)</b>
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

/** Jenkins Build 실행 중 오류 발생 시 발생하는 예외 */
public class JenkinsBuildException extends JenkinsException {

  public JenkinsBuildException(String message) {
    super(message);
  }

  public JenkinsBuildException(String message, Throwable cause) {
    super(message, cause);
  }
}
