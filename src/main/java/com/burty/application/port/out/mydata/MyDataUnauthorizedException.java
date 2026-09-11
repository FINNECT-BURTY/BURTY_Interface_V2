package com.burty.application.port.out.mydata;

/** 정보제공자가 접근토큰을 거절했다. 재발급하면 풀릴 수 있는 실패다. */
public class MyDataUnauthorizedException extends RuntimeException {
  public MyDataUnauthorizedException(String message) {
    super(message);
  }
}
