package com.burty.core.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.burty.util.PiiMasker;

/**
 * 로그 메시지에서 개인정보를 제거하는 logback 변환기.
 *
 * <p>호출부에서 {@code PiiMasker.account(...)} 로 명시적으로 가리는 것이 원칙이지만, 그것만으로는 부족하다. 로그 문장은 계속 늘어나고 그때마다
 * 마스킹을 기억할 수 없으며, 무엇보다 <b>예외 메시지는 우리가 문장을 통제할 수 없다.</b> DB 제약 위반 메시지에는 위반한 컬럼 값이 그대로 들어가고, 외부 API
 * 오류에는 응답 본문이 섞여 들어온다.
 *
 * <p>이 변환기는 최종 출력 직전에 한 번 더 훑는 안전망이다. 정규식이므로 완벽하지 않다. 명시적 마스킹을 대체하지 않는다.
 *
 * <p>logback 패턴에서 {@code %msg} 대신 {@code %maskedMsg} 로 쓴다.
 */
public class PiiMaskingConverter extends MessageConverter {

  @Override
  public String convert(ILoggingEvent event) {
    return PiiMasker.scrub(super.convert(event));
  }
}
