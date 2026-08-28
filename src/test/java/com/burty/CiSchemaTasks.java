package com.burty;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * CI 전용 스키마 작업.
 *
 * <p>백업·복구 검증 워크플로가 쓴다. 일반 테스트 실행에서는 태그로 제외된다.
 *
 * <p>애플리케이션의 Flyway 설정과 {@code ddl-auto=validate} 를 그대로 쓴다. 별도 스크립트로 스키마를 만들면 실제 애플리케이션이
 * 기대하는 것과 어긋날 수 있고, 그러면 복구 검증이 의미를 잃는다.
 *
 * <p>대상 데이터베이스는 시스템 프로퍼티로 받는다.
 *
 * <pre>
 *   ./gradlew ciSchema -Dburty.ci.db.url=jdbc:mariadb://127.0.0.1:3306/burty ...
 * </pre>
 *
 * <p>컨텍스트가 뜨는 것 자체가 검증이다. Flyway 가 적용되고 validate 를 통과해야만 뜬다.
 */
@SpringBootTest
@Tag("ci-schema")
class CiSchemaTasks {

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> required("burty.ci.db.url"));
    registry.add("spring.datasource.username", () -> required("burty.ci.db.user"));
    registry.add("spring.datasource.password", () -> System.getProperty("burty.ci.db.password", ""));
    registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    registry.add("spring.flyway.enabled", () -> System.getProperty("burty.ci.flyway", "true"));
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  private static String required(String key) {
    String value = System.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "시스템 프로퍼티 " + key + " 가 필요합니다 (CI 전용 작업입니다)");
    }
    return value;
  }

  @Test
  void applyMigrationsAndValidate() {
    // 컨텍스트가 떴다는 것은 Flyway 적용과 스키마 검증이 모두 통과했다는 뜻이다.
  }
}
