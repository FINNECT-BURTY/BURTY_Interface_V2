package com.burty.core.constant;

/** 사용자·운영 메시지 상수 (한글). */
public final class AppMessages {

  private AppMessages() {}

  public static final class Transfer {
    public static final String WEBAUTHN_VERIFY_FAILED = "FIDO2/WebAuthn 인증 검증에 실패했습니다.";
    public static final String LIMIT_NEGATIVE = "한도는 0 이상이어야 합니다.";
    public static final String DAILY_LIMIT_EXCEEDED = "1일 이체 한도를 초과했습니다.";
    public static final String INVALID_USER_ID = "유효하지 않은 사용자 ID입니다.";
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다.";
    public static final String FAMILY_ALERT_SUSPICIOUS = "[경고] 이상거래 의심: 심야/미등록계좌/대규모 이체 패턴 감지";
    public static final String FAMILY_ALERT_TRANSFER = "부모님 계정에서 %,d원이 %s 계좌로 이체되었습니다.";
    public static final String ORDER_FAILED_PREFIX = "이체 처리 실패: ";
    public static final String INVALID_AMOUNT = "이체 금액은 0보다 커야 합니다.";
    public static final String INVALID_ACCOUNT = "출금·입금 계좌 정보가 필요합니다.";
    public static final String OPENBANKING_NOT_LINKED = "오픈뱅킹 연동이 필요합니다. 먼저 계좌 연동을 완료해주세요.";

    private Transfer() {}
  }

  public static final class Identity {
    public static final String VERIFY_SUCCESS = "본인확인이 완료되었습니다.";
    public static final String VERIFY_FAILED = "본인확인에 실패했습니다.";
    public static final String CI_MISMATCH = "본인확인 정보가 일치하지 않습니다.";

    private Identity() {}
  }

  public static final class Cashflow {
    public static final String WHAT_IF_LABEL = "시나리오: %s";

    private Cashflow() {}
  }

  public static final class MyData {
    public static final String TOKEN_EXCHANGE_WARN =
        "마이데이터 토큰 교환 실패 userId={} institution={} err={}";

    private MyData() {}
  }
}
