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

  /**
   * 외부 Redis 주소. {@code host:port} 형식이며, 지정하면 컨테이너를 띄우지 않고 이 서버를 쓴다.
   *
   * <p>Docker 가 없는 환경에서도 Redis 계약 테스트를 돌릴 수 있게 하기 위한 탈출구다. 이 경로들은 운영에서만 살아나므로, 로컬에서 한 번도 못 돌리면 CI
   * 로그만 보고 고쳐야 한다.
   *
   * <pre>
   *   redis-server --port 6380 &amp;
   *   BURTY_TEST_REDIS=localhost:6380 ./gradlew test
   * </pre>
   */
  private static final String EXTERNAL = System.getenv("BURTY_TEST_REDIS");

  private static final boolean DOCKER_AVAILABLE = EXTERNAL == null && detectDocker();
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
    return INSTANCE != null || EXTERNAL != null;
  }

  /** 컨테이너에 연결된 템플릿. Docker 가 없으면 {@code null}. */
  public static synchronized StringRedisTemplate template() {
    if (INSTANCE == null && EXTERNAL == null) {
      return null;
    }
    if (template == null) {
      String host = INSTANCE != null ? INSTANCE.getHost() : EXTERNAL.split(":")[0];
      int port =
          INSTANCE != null
              ? INSTANCE.getMappedPort(6379)
              : Integer.parseInt(EXTERNAL.split(":")[1]);
      LettuceConnectionFactory factory =
          new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
      factory.afterPropertiesSet();
      StringRedisTemplate created = new StringRedisTemplate(factory);
      created.afterPropertiesSet();
      template = created;
    }
    return template;
  }
}
