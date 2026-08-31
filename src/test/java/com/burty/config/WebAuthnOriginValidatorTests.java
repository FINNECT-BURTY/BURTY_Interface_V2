package com.burty.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/**
 * origin·rpId 관계를 기동 시점에 확인한다.
 *
 * <p>이 검사가 있는 이유는 설정이 <b>틀렸는데도 아무것도 깨지지 않던</b> 상태 때문이다. prod 는 FE 와 BE 가 같은 도메인이라 우연히 맞았고,
 * dev·staging 은 스텁이 서명을 검증하지 않아 불일치가 드러나지 않았다.
 */
class WebAuthnOriginValidatorTests {

  private static WebAuthnProperties properties(String origin, String rpId) {
    WebAuthnProperties properties = new WebAuthnProperties();
    properties.setOrigin(origin);
    properties.setRpId(rpId);
    return properties;
  }

  /** 검사가 남긴 로그를 읽는다. 기동을 막지 않으므로 확인할 수 있는 것은 로그뿐이다. */
  private static List<ILoggingEvent> runAndCapture(WebAuthnProperties properties) {
    Logger logger = (Logger) LoggerFactory.getLogger(WebAuthnOriginValidator.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      new WebAuthnOriginValidator(properties).verify();
      return List.copyOf(appender.list);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  private static Level levelFor(String origin, String rpId) {
    List<ILoggingEvent> events = runAndCapture(properties(origin, rpId));
    assertEquals(1, events.size(), "검사는 결과를 정확히 한 줄로 남긴다");
    return events.getFirst().getLevel();
  }

  static Stream<Arguments> matching() {
    return Stream.of(
        Arguments.of("http://localhost:3000", "localhost"),
        Arguments.of("https://burty.co.kr", "burty.co.kr"),
        // FE 를 하위 도메인으로 옮기는 경우. WebAuthn 이 허용하는 관계다.
        Arguments.of("https://app.burty.co.kr", "burty.co.kr"));
  }

  @ParameterizedTest(name = "origin {0} / rpId {1}")
  @MethodSource("matching")
  @DisplayName("맞는 조합은 INFO 로 지나간다")
  void accepts(String origin, String rpId) {
    assertEquals(Level.INFO, levelFor(origin, rpId));
  }

  static Stream<Arguments> mismatched() {
    return Stream.of(
        // 예전 설정이 정확히 이 모양이었다. origin 에 백엔드 주소가 들어간 경우는 아니지만,
        // rpId 가 prod 값인데 origin 이 로컬이면 관계가 깨진다.
        Arguments.of("http://localhost:3000", "burty.co.kr"),
        Arguments.of("https://burty.co.kr", "example.com"),
        // 하위 도메인 관계는 방향이 있다. rpId 가 origin 의 하위일 수는 없다.
        Arguments.of("https://burty.co.kr", "app.burty.co.kr"),
        // 접미사만 같은 남의 도메인을 하위 도메인으로 착각하면 안 된다.
        Arguments.of("https://notburty.co.kr", "burty.co.kr"));
  }

  @ParameterizedTest(name = "origin {0} / rpId {1}")
  @MethodSource("mismatched")
  @DisplayName("어긋난 조합은 ERROR 로 드러낸다")
  void rejects(String origin, String rpId) {
    assertEquals(Level.ERROR, levelFor(origin, rpId));
  }

  @Test
  @DisplayName("URL 로 읽을 수 없는 origin 을 드러낸다")
  void reportsUnreadableOrigin() {
    // 스킴 없이 호스트만 넣은 흔한 실수. URI 는 이것을 path 로 읽어 host 가 null 이 된다.
    assertEquals(Level.ERROR, levelFor("localhost:3000", "localhost"));
    assertEquals(Level.ERROR, levelFor("", "localhost"));
  }

  @Test
  @DisplayName("기본값끼리는 맞는다")
  void defaultsAgree() {
    WebAuthnProperties defaults = new WebAuthnProperties();
    List<ILoggingEvent> events = runAndCapture(defaults);
    assertTrue(
        events.getFirst().getLevel() == Level.INFO, "기본값이 서로 어긋나면 아무 설정도 하지 않은 개발자가 곧바로 오류를 본다");
  }
}
