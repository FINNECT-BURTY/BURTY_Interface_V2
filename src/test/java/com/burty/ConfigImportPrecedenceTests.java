package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 프로파일 파일과 {@code spring.config.import} 의 우선순위.
 *
 * <p>모든 프로파일 파일이 {@code application-common.properties} 를 import 한다. 그래서 프로파일에서 공통값을 덮어쓰려 할 때 <b>어느
 * 쪽이 이기는지</b>가 설정 전체의 의미를 좌우한다.
 *
 * <p>스테이징에서 {@code burty.external.stub-mode=false} 를 적어두고도 실제로는 stub 응답이 나왔다. 즉 import 한 쪽이 이긴다.
 * 문제는 같은 구조를 prod 도 쓴다는 점이다 — prod 의 {@code stub-mode=false} 도 같은 이유로 무력할 수 있다.
 *
 * <p>추측으로 둘 문제가 아니라서 여기서 못 박는다. 동작이 바뀌면 이 테스트가 먼저 알려준다.
 */
class ConfigImportPrecedenceTests {

  @Configuration
  static class Empty {}

  private static ConfigurableApplicationContext contextFor(String profile) {
    SpringApplication app =
        new SpringApplicationBuilder(Empty.class)
            .web(WebApplicationType.NONE)
            .profiles(profile)
            .properties("spring.main.banner-mode=off")
            .build();
    return app.run();
  }

  @Test
  @DisplayName("프로파일 파일의 값이 import 한 공통값을 이긴다")
  void profileOverridesImportedCommon() {
    try (ConfigurableApplicationContext context = contextFor("staging")) {
      Environment env = context.getEnvironment();

      // application.properties         : burty.external.stub-mode=true
      // application-staging.properties  : burty.external.stub-mode=false
      //
      // 프로파일 쪽이 이겨야 스테이징이 존재 이유를 갖는다. 지면 스테이징은 dev 와 같다.
      assertEquals(
          "false",
          env.getProperty("burty.external.stub-mode"),
          "프로파일에서 덮어쓴 값이 무시된다 — 공통 기본값이 이기고 있다");
    }
  }

  @Test
  @DisplayName("prod 의 stub 해제도 실제로 적용된다")
  void prodStubOverridesApply() {
    try (ConfigurableApplicationContext context = contextFor("prod")) {
      Environment env = context.getEnvironment();

      // 여기가 지면 운영이 stub 으로 도는 것이다. ProdStartupValidator 가 막아주기는 하지만,
      // 막힌 이유를 설정 파일만 보고는 알 수 없다.
      assertEquals("false", env.getProperty("burty.mydata.stub-mode"));
      assertEquals("false", env.getProperty("burty.external.stub-mode"));
    }
  }
}
