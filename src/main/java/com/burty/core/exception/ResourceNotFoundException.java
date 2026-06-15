/**
 *
 *
 * <pre>
 * <b>Description  : 코어 예외 (ResourceNotFoundException)</b>
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

public class ResourceNotFoundException extends BusinessException {

  public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
    super(
        ErrorCode.ENTITY_NOT_FOUND,
        String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
  }

  public ResourceNotFoundException(String message) {
    super(ErrorCode.ENTITY_NOT_FOUND, message);
  }

  public ResourceNotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
