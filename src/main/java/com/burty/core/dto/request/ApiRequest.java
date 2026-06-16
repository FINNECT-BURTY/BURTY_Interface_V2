/**
 *
 *
 * <pre>
 * <b>Description  : 코어 요청 DTO (ApiRequest)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.dto.request
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
package com.burty.core.dto.request;

public class ApiRequest<T> {

  private T payload;

  public ApiRequest() {}

  public ApiRequest(T payload) {
    this.payload = payload;
  }

  public T getPayload() {
    return payload;
  }

  public void setPayload(T payload) {
    this.payload = payload;
  }
}
