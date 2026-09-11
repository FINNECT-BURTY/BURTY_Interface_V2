package com.burty.application.port.out.mydata;

/**
 * 정보제공자가 정상 응답을 주지 않았다.
 *
 * <p>표준 API 는 HTTP 상태와 별개로 {@code rsp_code} 로 결과를 준다. 200 이어도 여기로 온다.
 */
public class MyDataApiException extends RuntimeException {

  private final String responseCode;

  public MyDataApiException(String responseCode, String message) {
    super("rsp_code=" + responseCode + " " + message);
    this.responseCode = responseCode;
  }

  public String responseCode() {
    return responseCode;
  }
}
