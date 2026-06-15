/**
 *
 *
 * <pre>
 * <b>Description  : 헬스체크 API 컨트롤러 (HealthController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.health
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.in.web.health;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

  private final DataSource dataSource;
  private final RedisConnectionFactory redisConnectionFactory;
  private final boolean redisEnabled;

  public HealthController(
      DataSource dataSource,
      @Autowired(required = false) RedisConnectionFactory redisConnectionFactory,
      @Value("${burty.redis.enabled:false}") boolean redisEnabled) {
    this.dataSource = dataSource;
    this.redisConnectionFactory = redisConnectionFactory;
    this.redisEnabled = redisEnabled;
  }

  @GetMapping
  public Map<String, Object> health() {
    Map<String, Object> components = new LinkedHashMap<>();
    components.put("db", checkDatabase());
    components.put("redis", checkRedis());

    boolean up =
        "UP".equals(components.get("db"))
            && ("UP".equals(components.get("redis")) || "SKIPPED".equals(components.get("redis")));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", up ? "UP" : "DOWN");
    body.put("timestamp", System.currentTimeMillis());
    body.put("service", "BURTY API");
    body.put("components", components);
    return body;
  }

  @GetMapping("/ping")
  public Map<String, String> ping() {
    return Map.of("message", "pong");
  }

  private String checkDatabase() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(2) ? "UP" : "DOWN";
    } catch (Exception e) {
      return "DOWN";
    }
  }

  private String checkRedis() {
    if (!redisEnabled) {
      return "SKIPPED";
    }
    if (redisConnectionFactory == null) {
      return "DOWN";
    }
    try {
      redisConnectionFactory.getConnection().ping();
      return "UP";
    } catch (Exception e) {
      return "DOWN";
    }
  }
}
