package com.burty.adapter.out.mydata.standard;

/** 표준 API 응답 공통부. HTTP 상태와 별개로 {@code rsp_code} 가 결과를 말한다. */
public interface StandardResponse {

  String SUCCESS = "00000";

  String rspCode();

  String rspMsg();

  default boolean succeeded() {
    return SUCCESS.equals(rspCode());
  }
}
