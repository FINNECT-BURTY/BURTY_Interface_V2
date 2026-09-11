package com.burty;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.burty.support.IntegrationTestBase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 헬스체크 경로.
 *
 * <p>컨테이너 HEALTHCHECK, compose, nginx, Jenkins 배포 확인이 모두 {@code /health} 를 부른다. 컨트롤러에 {@code
 * /api/v1} 접두사가 붙으면서 그 경로가 사라졌는데, 운영 쪽 파일은 그대로여서 헬스체크가 전부 실패하고 있었다. 코드만 봐서는 드러나지 않으므로 운영 파일이 부르는
 * 경로를 직접 읽어 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTests extends IntegrationTestBase {

  private static final List<String> OPS_FILES =
      List.of(
          "Dockerfile",
          "docker-compose.yml",
          "Jenkinsfile",
          "Jenkinsfile.compose",
          "infra/global-nginx/nginx.conf");

  /** 백엔드로 가는 헬스체크 URL 의 경로 부분. 모의 서버(WireMock)의 {@code /__admin/health} 는 제외된다. */
  private static final Pattern HEALTH_URL =
      Pattern.compile("(?:localhost:8080|\\$\\{HOST}|\\$\\{DOMAIN}|burty:8080)(/health[\\w/-]*)");

  private static final Pattern NGINX_HEALTH_LOCATION =
      Pattern.compile("location\\s*=\\s*(/health[\\w/-]*)");

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("운영 파일이 부르는 헬스체크 경로가 무인증으로 UP 을 준다")
  void opsHealthcheckPathsAreServed() throws Exception {
    Set<String> paths = opsHealthPaths();
    assertFalse(paths.isEmpty(), "운영 파일에서 헬스체크 경로를 찾지 못했다");

    for (String path : paths) {
      mockMvc
          .perform(get(path))
          .andExpect(status().isOk())
          .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }
  }

  @Test
  @DisplayName("없는 경로는 404 다 — 서버 오류로 답하지 않는다")
  void unknownPathIsNotFound() throws Exception {
    // 500 으로 답하면 모니터링에는 장애로 잡히고, 로그에는 스택과 함께 ERROR 가 남는다.
    mockMvc.perform(get("/health/does-not-exist")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("/actuator/health 는 무인증으로 UP 이다")
  void actuatorHealthIsUp() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("readiness 는 UP 이다")
  void readinessIsUp() throws Exception {
    mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  private static Set<String> opsHealthPaths() throws IOException {
    Set<String> paths = new TreeSet<>();
    for (String file : OPS_FILES) {
      String text = Files.readString(Path.of(file));
      for (Pattern pattern : List.of(HEALTH_URL, NGINX_HEALTH_LOCATION)) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
          paths.add(matcher.group(1));
        }
      }
    }
    return paths;
  }
}
