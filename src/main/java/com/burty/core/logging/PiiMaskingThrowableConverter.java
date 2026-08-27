package com.burty.core.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.burty.util.PiiMasker;

/**
 * 스택트레이스에서도 개인정보를 제거한다.
 *
 * <p>메시지만 가리고 스택트레이스를 그대로 두면 의미가 없다. 예외 메시지는 스택트레이스 첫 줄에 그대로 다시 나오고, {@code
 * DataIntegrityViolationException} 의 경우 SQL 과 바인딩된 값까지 포함된다.
 */
public class PiiMaskingThrowableConverter extends ThrowableProxyConverter {

  @Override
  protected String throwableProxyToString(ch.qos.logback.classic.spi.IThrowableProxy tp) {
    return PiiMasker.scrub(super.throwableProxyToString(tp));
  }

  @Override
  public String convert(ILoggingEvent event) {
    return PiiMasker.scrub(super.convert(event));
  }
}
