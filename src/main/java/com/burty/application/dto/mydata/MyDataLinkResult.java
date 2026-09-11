package com.burty.application.dto.mydata;

/**
 * 인가 콜백의 결과. FE 연동 관리 화면으로 돌려보낼 때 쓴다.
 *
 * @param institutionCode state 로 복원한 기관. state 가 유효하지 않으면 알 수 없어 null 이다.
 * @param frontendOrigin 인가 요청 때 묶어 둔 FE. 없으면 기본 FE 로 보낸다.
 */
public record MyDataLinkResult(Outcome outcome, String institutionCode, String frontendOrigin) {

  public enum Outcome {
    LINKED,
    /** 사용자가 정보제공자 화면에서 거절했다. */
    DENIED,
    /** state 가 없거나 만료됐거나 이미 쓰였다. */
    STATE_INVALID,
    /** 토큰 교환에 실패했다. */
    EXCHANGE_FAILED
  }

  public static MyDataLinkResult stateInvalid() {
    return new MyDataLinkResult(Outcome.STATE_INVALID, null, null);
  }
}
