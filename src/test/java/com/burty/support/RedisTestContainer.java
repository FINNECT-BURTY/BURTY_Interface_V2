package com.burty.support;

import java.time.Duration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * 계약 테스트용 Redis.
 *
 * <p>운영에서 도는 것은 {@code Redis*} 구현인데, 테스트는 {@code burty.redis.enabled=false} 라 늘 in-memory 구현만 검증했다.
 * 실제로 캐시 직렬화 결함이 그렇게 운영에서만 드러났다.
 *
 * <p>Docker 가 있으면(CI, 개발자 로컬) 진짜 Redis 를 띄워 양쪽 구현에 같은 계약을 건다. 없으면 Redis 쪽은 건너뛴다 — MariaDB 컨테이너와 같은
 * 방식이다.
 */
public final class RedisTestContainer {

  private static final boolean DOCKER_AVAILABLE = detectDocker();
  private static final GenericContainer<?> INSTANCE = DOCKER_AVAILABLE ? start() : null;
  private static StringRedisTemplate template;

  private RedisTestContainer() {}

  private static boolean detectDocker() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (RuntimeException | LinkageError e) {
      return false;
    }
  }

  private static GenericContainer<?> start() {
    GenericContainer<?> container =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
    container.start();
    return container;
  }

  public static boolean isAvailable() {
    return INSTANCE != null;
  }

  /** 컨테이너에 연결된 템플릿. Docker 가 없으면 {@code null}. */
  public static synchronized StringRedisTemplate template() {
    if (INSTANCE == null) {
      return null;
    }
    if (template == null) {
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(
              new RedisStandaloneConfiguration(INSTANCE.getHost(), INSTANCE.getMappedPort(6379)));
      factory.afterPropertiesSet();
      StringRedisTemplate created = new StringRedisTemplate(factory);
      created.afterPropertiesSet();
      template = created;
    }
    return template;
  }
}
