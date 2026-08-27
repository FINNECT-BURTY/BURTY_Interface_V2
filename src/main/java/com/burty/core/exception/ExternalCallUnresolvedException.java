package com.burty.core.exception;

/**
 * 외부 시스템 호출의 <b>결과를 알 수 없는</b> 상태.
 *
 * <p>타임아웃, 커넥션 끊김, 5xx 처럼 "요청이 상대에게 도달했는지조차 불확실한" 경우에 던진다. 명확한 업무 거절(4xx)과 반드시 구분해야 한다. 이체에서 이 둘을
 * 뭉뚱그리면, 실제로는 출금된 건을 실패로 기록해 사용자에게 돈을 두 번 빼앗는 결과가 나온다.
 *
 * <p>호출자는 이 예외를 받으면 상태를 {@code UNKNOWN} 으로 남기고 정산(reconciliation) 대상으로 넘겨야 한다.
 */
public class ExternalCallUnresolvedException extends RuntimeException {

  private final String operation;

  public ExternalCallUnresolvedException(String operation, String message, Throwable cause) {
    super(message, cause);
    this.operation = operation;
  }

  public String getOperation() {
    return operation;
  }
}
