package com.burty.domain.mydata.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 수신계좌 거래 한 건. 금액은 항상 양수이고 방향은 {@link Direction} 으로 가른다. */
public record BankTransaction(
    LocalDateTime occurredAt,
    String transactionNo,
    Direction direction,
    String transactionClass,
    BigDecimal amount,
    BigDecimal balanceAfter) {

  public enum Direction {
    DEPOSIT,
    WITHDRAWAL,
    /** 규격의 거래유형 중 입금·출금으로 가를 수 없는 것. 지출 합계에 넣지 않는다. */
    OTHER
  }

  public boolean isWithdrawal() {
    return direction == Direction.WITHDRAWAL;
  }
}
