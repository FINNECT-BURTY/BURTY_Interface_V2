/**
 *
 *
 * <pre>
 * <b>Description  : 코어 예외 (AuthenticationException)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.exception
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
package com.burty.core.exception;

import com.burty.core.error.enums.ErrorCode;

public class AuthenticationException extends BusinessException {

  public AuthenticationException(ErrorCode errorCode) {
    super(errorCode);
  }

  public AuthenticationException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public AuthenticationException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }

  public AuthenticationException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
