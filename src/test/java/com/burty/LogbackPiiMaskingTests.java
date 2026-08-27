package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * logback 마스킹 계층이 실제로 동작하는지 검증한다.
 *
 * <p>{@link PiiMaskerTests} 가 마스킹 <b>로직</b>을 검증한다면, 여기서는 그 로직이 <b>실제 로그 출력 경로에 연결되어 있는지</b>를 본다.
 * `logback-spring.xml` 의 conversionRule 이름을 한 글자만 잘못 써도 마스킹이 통째로 무력화되는데, 그건 운영에 나가서야 드러난다.
 */
class LogbackPiiMaskingTests {

  private static final String PATTERN = "%maskedMsg%n%maskedEx";

  @Test
  @DisplayName("변환기를 등록하면 로그 메시지의 개인정보가 가려진다")
  void configuredConverterMasksMessage() {
    String rendered = renderWithMaskingPattern("이체 실패 계좌=110234567890 전화=010-1234-5678");

    assertFalse(rendered.contains("110234567890"), "계좌번호가 로그에 남았습니다: " + rendered);
    assertFalse(rendered.contains("010-1234-5678"), "전화번호가 로그에 남았습니다: " + rendered);
    assertTrue(rendered.contains("***7890"));
    assertTrue(rendered.contains("***5678"));
  }

  @Test
  @DisplayName("스택트레이스의 개인정보도 가려진다")
  void configuredConverterMasksStackTrace() {
    // 예외 메시지는 스택트레이스 첫 줄에 그대로 다시 나온다.
    // 메시지만 가리고 스택트레이스를 두면 의미가 없다.
    Exception cause =
        new IllegalStateException("Duplicate entry '110234567890' for key 'uk_account'");
    String rendered = renderWithMaskingPattern("계좌 등록 실패", cause);

    assertFalse(rendered.contains("110234567890"), "스택트레이스에 계좌번호가 남았습니다");
    assertTrue(rendered.contains("***7890"));
  }

  @Test
  @DisplayName("logback-spring.xml 이 마스킹 변환기를 사용한다")
  void productionConfigUsesMaskingConverter() throws Exception {
    String config =
        new String(
            getClass().getResourceAsStream("/logback-spring.xml").readAllBytes(),
            StandardCharsets.UTF_8);

    assertTrue(
        config.contains("com.burty.core.logging.PiiMaskingConverter"), "메시지 마스킹 변환기가 등록되어 있지 않습니다");
    assertTrue(
        config.contains("com.burty.core.logging.PiiMaskingThrowableConverter"),
        "스택트레이스 마스킹 변환기가 등록되어 있지 않습니다");
    assertFalse(config.contains("%msg%n"), "마스킹되지 않는 %msg 패턴이 남아 있습니다. %maskedMsg 를 사용해야 합니다");
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  private String renderWithMaskingPattern(String message) {
    return renderWithMaskingPattern(message, null);
  }

  /**
   * 운영 설정과 동일한 conversionRule 로 로거를 구성해 실제 렌더링 결과를 얻는다.
   *
   * <p><b>전역 LoggerContext 를 건드리지 않는다.</b> 전역 컨텍스트의 패턴 규칙을 바꾸면 같은 JVM 에서 도는 다른 테스트는 물론 Gradle 테스트
   * 워커의 자체 로깅까지 영향을 받아 워커가 죽는다(EOFException). 격리된 컨텍스트를 새로 만든다.
   */
  private String renderWithMaskingPattern(String message, Throwable throwable) {
    LoggerContext context = new LoggerContext();
    context.start();
    try {
      java.util.Map<String, String> rules = new java.util.HashMap<>();
      rules.put("maskedMsg", "com.burty.core.logging.PiiMaskingConverter");
      rules.put("maskedEx", "com.burty.core.logging.PiiMaskingThrowableConverter");
      context.putObject(ch.qos.logback.core.CoreConstants.PATTERN_RULE_REGISTRY, rules);

      PatternLayoutEncoder encoder = new PatternLayoutEncoder();
      encoder.setContext(context);
      encoder.setPattern(PATTERN);
      encoder.start();

      ListAppender<ILoggingEvent> appender = new ListAppender<>();
      appender.setContext(context);
      appender.start();

      Logger logger = context.getLogger("com.burty.test.masking");
      logger.setLevel(Level.INFO);
      logger.setAdditive(false);
      logger.addAppender(appender);

      if (throwable == null) {
        logger.info(message);
      } else {
        logger.info(message, throwable);
      }
      return new String(encoder.encode(appender.list.get(0)), StandardCharsets.UTF_8);
    } finally {
      context.stop();
    }
  }
}
