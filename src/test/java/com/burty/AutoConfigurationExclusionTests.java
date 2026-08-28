package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.support.IntegrationTestBase;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 자동설정 제외가 실제로 걸리는지 검증한다.
 *
 * <p>{@code spring.autoconfigure.exclude} 는 클래스를 못 찾으면 <b>조용히 무시된다.</b> 스프링 부트가 자동설정을 기술별 모듈로 쪼개면서
 * 클래스 위치가 여러 번 바뀌었고, 그때마다 설정 파일의 옛 이름은 아무 일도 하지 않는 문자열이 된다.
 *
 * <p>이 프로젝트에서 이미 두 번 겪었다.
 *
 * <ul>
 *   <li>{@code spring-boot-flyway} 모듈이 없어 Flyway 가 한 번도 돌지 않았다
 *   <li>{@code RedisAutoConfiguration} 이 {@code DataRedisAutoConfiguration} 으로 옮겨가, 제외가 걸린 줄 알았던
 *       Redis 자동설정이 계속 살아 요청마다 localhost:6379 접속을 시도하고 있었다
 * </ul>
 *
 * <p>둘 다 예외도 경고도 남기지 않는다. 그래서 테스트로 못 박는다.
 */
@SpringBootTest
class AutoConfigurationExclusionTests extends IntegrationTestBase {

  private static final String IMPORTS_RESOURCE =
      "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

  @Autowired private Environment environment;
  @Autowired private ApplicationContext context;

  @Test
  @DisplayName("제외 목록의 클래스가 전부 실재하는 자동설정이다")
  void excludedClassesAreRealAutoConfigurations() {
    String raw = environment.getProperty("spring.autoconfigure.exclude");
    assertNotNull(raw, "제외 설정이 사라졌다. 의도한 것이라면 이 테스트도 함께 정리할 것");

    Set<String> known = loadKnownAutoConfigurations();
    assertTrue(known.size() > 100, "자동설정 목록을 읽지 못했다 (읽은 개수: " + known.size() + ")");

    for (String name : Arrays.stream(raw.split(",")).map(String::trim).toList()) {
      if (name.isEmpty()) {
        continue;
      }
      // 이름만 맞고 클래스가 없으면 제외는 아무 일도 하지 않는다.
      assertTrue(
          known.contains(name),
          "자동설정 클래스가 아니거나 클래스패스에 없다: " + name + " — 이 이름으로는 제외가 걸리지 않는다. 스프링 부트가 클래스를 옮겼는지 확인할 것");
    }
  }

  @Test
  @DisplayName("테스트 환경에는 Redis 커넥션 팩토리가 없다")
  void noRedisConnectionFactoryInTests() {
    // 빈이 살아 있으면 JwtBlacklistService·PasswordAttemptTracker 가 in-memory fallback 이 아니라
    // 실제 Redis 접속을 시도한다. 매 요청마다 접속 실패 후 예외로 폴백하는 셈이라,
    // 테스트가 무엇을 검증하는지도 흐려진다.
    assertEquals(
        0,
        context.getBeanNamesForType(RedisConnectionFactory.class).length,
        "Redis 자동설정이 제외되지 않았다");
  }

  /** 클래스패스의 모든 jar 가 선언한 자동설정 클래스 이름. */
  private static Set<String> loadKnownAutoConfigurations() {
    Set<String> names = new HashSet<>();
    try {
      Enumeration<java.net.URL> resources =
          AutoConfigurationExclusionTests.class.getClassLoader().getResources(IMPORTS_RESOURCE);
      while (resources.hasMoreElements()) {
        try (InputStream in = resources.nextElement().openStream()) {
          String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          List<String> lines = body.lines().map(String::trim).toList();
          for (String line : lines) {
            if (!line.isEmpty() && !line.startsWith("#")) {
              names.add(line);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("자동설정 목록을 읽지 못했다", e);
    }
    return names;
  }
}
