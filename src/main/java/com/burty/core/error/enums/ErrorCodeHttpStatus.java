/**
 *
 *
 * <pre>
 * <b>Description  : 코어 (ErrorCodeHttpStatus)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.error.enums
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
package com.burty.core.error.enums;

import org.springframework.http.HttpStatus;

public final class ErrorCodeHttpStatus {

  private ErrorCodeHttpStatus() {}

  public static HttpStatus resolve(ErrorCode errorCode) {
    return switch (errorCode) {
      case UNAUTHORIZED, INVALID_TOKEN, EXPIRED_TOKEN, INVALID_CREDENTIALS, EMAIL_NOT_VERIFIED ->
          HttpStatus.UNAUTHORIZED;
      case FORBIDDEN, INSUFFICIENT_PERMISSIONS, OPERATION_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
      case ENTITY_NOT_FOUND, USER_NOT_FOUND, DATA_NOT_FOUND, FILE_NOT_FOUND, RESOURCE_NOT_FOUND ->
          HttpStatus.NOT_FOUND;
      case DUPLICATE_RESOURCE, USER_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, DATA_ALREADY_EXISTS ->
          HttpStatus.CONFLICT;
      case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
      case TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
      case INTERNAL_SERVER_ERROR,
              FILE_UPLOAD_FAILED,
              FILE_DOWNLOAD_FAILED,
              PDF_GENERATION_FAILED,
              EXTERNAL_API_ERROR,
              EMAIL_SEND_FAILED,
              JSON_PROCESSING_ERROR ->
          HttpStatus.INTERNAL_SERVER_ERROR;
      default -> HttpStatus.BAD_REQUEST;
    };
  }
}
