package com.burty.application.dto.auth;

/**
 * 가입할 때 받은 항목별 동의.
 *
 * <p>예전에는 화면이 여섯 항목에 동의를 받고 백엔드로는 {@code termsAccepted=true} 하나만 보냈다. 그래서 이용약관과 개인정보 처리 안내 두 건만
 * 기록되고 수집·이용, 전송요구, 마케팅, 혜택 동의는 기록이 남지 않았다.
 *
 * <p>동의는 "받았다" 가 아니라 <b>무엇에, 언제, 어떤 버전으로 받았는지 증명할 수 있다</b> 가 핵심이다. 특히 선택 동의(마케팅)는 기록이 없으면 수신 거부
 * 사용자에게 보내지 않았음을 증명할 수 없다.
 *
 * @param terms 서비스 이용약관 (필수)
 * @param privacy 개인정보 처리 안내 (필수)
 * @param creditCollection 개인신용정보 수집·이용 (필수)
 * @param creditTransfer 개인신용정보 전송요구 (필수)
 * @param marketing 마케팅 정보 수신 (선택)
 * @param benefit 맞춤 혜택 알림 수신 (선택)
 * @param overseasTransfer 개인정보 국외 이전 — AI 상담·음성 (선택)
 */
public record SignupConsents(
    boolean terms,
    boolean privacy,
    boolean creditCollection,
    boolean creditTransfer,
    boolean marketing,
    boolean benefit,
    boolean overseasTransfer) {

  /**
   * 항목별 값을 보내지 않는 옛 클라이언트용.
   *
   * <p>FE 와 백엔드는 따로 배포된다. 항목별 값이 오지 않는 동안에도 가입이 막히면 안 되므로, 예전과 같이 필수 약관 두 건만 기록한다. 이 경로로 들어온 가입은
   * 수집·이용과 전송요구 동의 기록이 없다는 뜻이다.
   */
  public static SignupConsents legacy(boolean termsAccepted) {
    return new SignupConsents(termsAccepted, termsAccepted, false, false, false, false, false);
  }
}
