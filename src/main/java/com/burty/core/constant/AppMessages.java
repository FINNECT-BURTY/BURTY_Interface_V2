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
    public static final String PER_TX_LIMIT_EXCEEDED = "1회 이체 한도를 초과했습니다.";
    public static final String RESULT_UNKNOWN =
        "이체 요청은 접수되었으나 은행 응답을 확인하지 못했습니다. 결과 확인 후 내역에 반영됩니다.";
    public static final String IN_PROGRESS = "동일한 멱등키의 이체가 아직 처리 중입니다.";
    public static final String NOT_FOUND = "이체 내역을 찾을 수 없습니다.";
    public static final String NOT_CANCELLABLE = "이미 실행되었거나 취소할 수 없는 이체입니다.";
    public static final String CANCELLED_BY_USER = "사용자 요청으로 취소되었습니다.";
    public static final String APPROVAL_REQUIRED = "보호자 승인이 필요한 금액입니다. 보호자에게 승인 요청을 보냈습니다.";

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
