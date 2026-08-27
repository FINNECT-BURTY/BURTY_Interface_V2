package com.burty.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MariaDBContainer;

/**
 * 통합 테스트용 데이터베이스 선택기.
 *
 * <p><b>Docker 가 있으면</b> (CI, 개발자 로컬) 운영과 동일한 MariaDB 컨테이너를 띄우고 Flyway 마이그레이션을 적용한 뒤 {@code
 * ddl-auto=validate} 로 검증한다. 즉 JPA 엔티티와 {@code db/migration} 이 어긋나면 테스트가 실패한다. 기존 H2 + {@code
 * create-drop} 조합에서는 구조적으로 잡을 수 없던 드리프트다.
 *
 * <p><b>Docker 가 없으면</b> H2(MariaDB 호환 모드) + {@code create-drop} 으로 자동 강등한다. 테스트는 계속 돌지만 스키마 검증 효과는
 * 없다. 스키마 드리프트 검증은 CI 게이트에 의존한다.
 *
 * <p>컨테이너는 JVM 당 한 번만 기동되며 Testcontainers 의 ryuk 가 종료 시 정리한다.
 */
public final class MariaDbTestContainer {

  private static final boolean DOCKER_AVAILABLE = detectDocker();
  private static final MariaDBContainer<?> INSTANCE = DOCKER_AVAILABLE ? startContainer() : null;

  private MariaDbTestContainer() {}

  private static boolean detectDocker() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (RuntimeException | LinkageError e) {
      return false;
    }
  }

  private static MariaDBContainer<?> startContainer() {
    MariaDBContainer<?> container =
        new MariaDBContainer<>("mariadb:11.4").withDatabaseName("burty").withReuse(true);
    container.start();
    return container;
  }

  public static boolean isRealDatabase() {
    return INSTANCE != null;
  }

  /** 데이터소스 + 스키마 관리 전략을 테스트 컨텍스트에 주입한다. */
  public static void registerProperties(DynamicPropertyRegistry registry) {
    if (INSTANCE == null) {
      registry.add(
          "spring.datasource.url", () -> "jdbc:h2:mem:burty;MODE=MariaDB;DB_CLOSE_DELAY=-1");
      registry.add("spring.datasource.username", () -> "sa");
      registry.add("spring.datasource.password", () -> "");
      registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
      registry.add("spring.flyway.enabled", () -> "false");
      registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
      return;
    }
    registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
    registry.add("spring.datasource.username", INSTANCE::getUsername);
    registry.add("spring.datasource.password", INSTANCE::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }
}
