package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스테이징 프로파일 설정 검증.
 *
 * <p>스테이징의 존재 이유는 하나다 — <b>외부 연동이 실제 HTTP 를 타게 하는 것.</b> stub 으로 돌면 어댑터가 HTTP 를 아예 타지 않고 가짜 객체를
 * 돌려주므로 직렬화·타임아웃·에러 매핑·서킷브레이커가 전부 검증되지 않는다. 운영에서 문제가 되는 것은 대부분 그 경로다.
 *
 * <p>그래서 stub 이 다시 켜지면 스테이징은 dev 와 같아진다. 실수로 되돌아가는 것을 여기서 막는다.
 *
 * <p>컨텍스트를 띄우지 않고 파일만 읽는다. 스테이징은 MariaDB·Redis·WireMock 이 전부 있어야 뜨므로 단위 테스트에서 기동시킬 수 없다.
 */
class StagingProfileTests {

  private static Properties staging() {
    Properties props = new Properties();
    try (InputStream in =
        StagingProfileTests.class.getResourceAsStream("/application-staging.properties")) {
      if (in == null) {
        throw new IllegalStateException("application-staging.properties 가 없다");
      }
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return props;
  }

  @Test
  @DisplayName("금융 연동은 stub 이 아니다 — 스테이징의 존재 이유다")
  void financialIntegrationsAreNotStubbed() {
    Properties props = staging();

    assertEquals("false", props.getProperty("burty.external.stub-mode"), "오픈뱅킹이 stub 이다");
    assertEquals("false", props.getProperty("burty.mydata.stub-mode"), "마이데이터가 stub 이다");
    assertEquals("false", props.getProperty("burty.identity.stub-mode"), "본인확인이 stub 이다");
  }

  @Test
  @DisplayName("사람에게 도달하는 채널은 stub 으로 둔다")
  void outboundToRealPeopleStaysStubbed() {
    Properties props = staging();

    // 스테이징에서 실제 발송이 나가면 사람이 알림을 받는다.
    assertEquals("true", props.getProperty("burty.notify.email.stub-mode"));
    assertEquals("true", props.getProperty("burty.notify.sms.stub-mode"));
    assertEquals("true", props.getProperty("burty.notify.push.stub-mode"));
  }

  @Test
  @DisplayName("외부 URL 이 목을 향한다")
  void externalUrlsPointAtTheMock() {
    Properties props = staging();

    for (String key :
        new String[] {
          "burty.external.open-banking-transfer-url",
          "burty.external.open-banking-token-url",
          "burty.external.open-banking-accounts-url",
          "burty.mydata.token-url",
          "burty.mydata.asset-url"
        }) {
      String value = props.getProperty(key);
      assertTrue(value != null && value.contains("MOCK_BANK_URL"), key + " 가 목을 향하지 않는다: " + value);
      assertFalse(value.contains("openbanking.or.kr"), key + " 가 실제 기관을 향한다: " + value);
    }
  }

  @Test
  @DisplayName("운영과 같은 저장소 구성을 쓴다")
  void usesProductionLikeInfrastructure() {
    Properties props = staging();

    // in-memory fallback 으로 도는 경로는 운영에 없다. 그 경로를 검증해봐야 소용없다.
    assertEquals("true", props.getProperty("burty.redis.enabled"), "Redis 가 꺼져 있다");
    assertEquals("validate", props.getProperty("spring.jpa.hibernate.ddl-auto"));
    assertEquals("true", props.getProperty("spring.flyway.enabled"));
  }
}
