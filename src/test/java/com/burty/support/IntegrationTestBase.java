package com.burty.support;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 통합 테스트 공통 베이스.
 *
 * <p>{@code @SpringBootTest} 는 각 테스트가 직접 선언한다 (webEnvironment 등 옵션이 테스트마다 다르기 때문). 이 클래스는 데이터소스와
 * 스키마 전략 주입만 담당한다. 상세는 {@link MariaDbTestContainer}.
 *
 * <p>여기를 상속하면 {@code integration} 태그가 붙는다(JUnit 은 클래스 태그를 상속한다). 스프링 컨텍스트와 Testcontainers 가 필요한
 * 테스트라 느리므로, 기본 {@code test} 태스크에서는 제외하고 {@code integrationTest} 로 따로 돌린다. {@code check} 는 둘 다
 * 실행한다.
 */
@Tag("integration")
public abstract class IntegrationTestBase {

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    MariaDbTestContainer.registerProperties(registry);
  }
}
