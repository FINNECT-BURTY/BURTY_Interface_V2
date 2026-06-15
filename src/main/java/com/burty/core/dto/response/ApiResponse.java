/**
 *
 *
 * <pre>
 * <b>Description  : 코어 응답 DTO (ApiResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.dto.response
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
package com.burty.core.dto.response;

public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;
  private String errorCode;

  public ApiResponse() {}

  public ApiResponse(boolean success, String message, T data, String errorCode) {
    this.success = success;
    this.message = message;
    this.data = data;
    this.errorCode = errorCode;
  }

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "Success", data, null);
  }

  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return new ApiResponse<>(false, message, null, errorCode);
  }

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, "Success", data, null);
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }
}
