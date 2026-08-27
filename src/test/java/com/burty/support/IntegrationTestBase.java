package com.burty.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 통합 테스트 공통 베이스.
 *
 * <p>{@code @SpringBootTest} 는 각 테스트가 직접 선언한다 (webEnvironment 등 옵션이 테스트마다 다르기 때문). 이 클래스는 데이터소스와
 * 스키마 전략 주입만 담당한다. 상세는 {@link MariaDbTestContainer}.
 */
public abstract class IntegrationTestBase {

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    MariaDbTestContainer.registerProperties(registry);
  }
}
