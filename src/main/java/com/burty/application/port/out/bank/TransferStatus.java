package com.burty.application.port.out.bank;

/** 은행 측에 조회한 이체 건의 최종 상태. 정산 배치가 사용한다. */
public record TransferStatus(Outcome outcome, String bankTransactionId, String reason) {

  public enum Outcome {
    /** 은행이 정상 처리함 — 출금 완료. */
    COMPLETED,
    /** 은행이 거절함 — 출금 없음. */
    REJECTED,
    /** 은행에 해당 건이 없음 — 요청이 도달하지 못함. 출금 없음. */
    NOT_FOUND,
    /** 아직 은행에서도 처리 중. 다음 주기에 다시 조회. */
    PENDING,
    /** 조회 자체가 실패 — 판단 불가. */
    UNRESOLVED
  }

  public static TransferStatus completed(String bankTransactionId) {
    return new TransferStatus(Outcome.COMPLETED, bankTransactionId, null);
  }

  public static TransferStatus rejected(String reason) {
    return new TransferStatus(Outcome.REJECTED, null, reason);
  }

  public static TransferStatus notFound() {
    return new TransferStatus(Outcome.NOT_FOUND, null, "은행에 해당 거래가 존재하지 않음");
  }

  public static TransferStatus pending() {
    return new TransferStatus(Outcome.PENDING, null, null);
  }

  public static TransferStatus unresolved(String reason) {
    return new TransferStatus(Outcome.UNRESOLVED, null, reason);
  }
}
