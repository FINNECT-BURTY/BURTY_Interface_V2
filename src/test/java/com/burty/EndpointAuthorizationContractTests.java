package com.burty;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.burty.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 모든 엔드포인트가 인증 없이는 열리지 않는지 검사한다.
 *
 * <p>컨트롤러 45개에 엔드포인트가 150개가 넘는데, 테스트가 실제로 호출하는 것은 열두 개였다. 나머지는 인가가 걸려 있는지 아무도 확인한 적이 없다.
 *
 * <p><b>엔드포인트를 하나씩 적지 않는다.</b> 목록을 손으로 관리하면 새 컨트롤러가 추가될 때 목록에 넣는 것을 잊고, 잊은 엔드포인트는 정확히 검사받지 않는
 * 엔드포인트가 된다. 스프링의 핸들러 매핑에서 실제 등록된 경로를 읽어 전수 검사한다.
 *
 * <p>공개여야 하는 경로만 아래 목록으로 관리한다. 이 목록에 새 경로를 넣는 것은 "이 경로를 무인증으로 연다" 는 명시적 결정이고, 리뷰에서 눈에 띈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndpointAuthorizationContractTests extends IntegrationTestBase {

  /**
   * 무인증 허용 경로.
   *
   * <p>{@code SecurityConfig} 의 permitAll 규칙과 짝을 이룬다. 여기 없는 경로가 무인증으로 열려 있으면 실패한다.
   */
  private static final List<Pattern> PUBLIC_PATHS =
      List.of(
          Pattern.compile("^/health(/.*)?$"),
          Pattern.compile("^/actuator/health(/.*)?$"),
          Pattern.compile("^/actuator/prometheus$"),
          Pattern.compile("^/api/v1/auth/token$"),
          Pattern.compile("^/api/v1/auth/refresh$"),
          Pattern.compile("^/api/v1/auth/logout$"),
          Pattern.compile("^/api/v1/auth/[^/]+/authorize-url$"),
          Pattern.compile("^/api/v1/auth/[^/]+/callback$"),
          Pattern.compile("^/api/v1/auth/[^/]+/login$"),
          Pattern.compile("^/api/v1/auth/demo/.*$"),
          Pattern.compile("^/api/v1/admin/auth/login$"),
          Pattern.compile("^/api/v1/admin/auth/refresh$"),
          Pattern.compile("^/api/v1/admin/auth/register$"),
          Pattern.compile("^/api/v1/sessions/refresh$"),
          Pattern.compile("^/api/v1/external/openbanking/oauth/callback$"),
          Pattern.compile("^/api/v1/mydata/oauth/callback$"),
          Pattern.compile("^/api/v1/mydata/institutions/[^/]+/callback$"),
          // Swagger 는 운영에서 꺼진다 (ProdStartupValidator 가 강제).
          Pattern.compile("^/api/v1/swagger-ui.*$"),
          Pattern.compile("^/api/v1/v3/api-docs.*$"),
          Pattern.compile("^/swagger-ui.*$"),
          Pattern.compile("^/v3/api-docs.*$"),
          Pattern.compile("^/swagger-resources/.*$"),
          Pattern.compile("^/webjars/.*$"),
          Pattern.compile("^/error$"));

  @Autowired private MockMvc mockMvc;

  // actuator 도 같은 타입의 빈을 등록하므로 이름으로 지목한다.
  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Test
  @DisplayName("공개 목록에 없는 모든 엔드포인트는 인증 없이 접근할 수 없다")
  void everyEndpointRequiresAuthentication() throws Exception {
    List<Endpoint> endpoints = mappedEndpoints();
    assertTrue(endpoints.size() > 100, "엔드포인트를 읽지 못했다 (읽은 개수: " + endpoints.size() + ")");

    List<String> leaked = new ArrayList<>();

    for (Endpoint endpoint : endpoints) {
      if (isPublic(endpoint.path())) {
        continue;
      }

      MvcResult result = mockMvc.perform(request(endpoint)).andReturn();
      int status = result.getResponse().getStatus();

      // 401·403 이면 막힌 것이다. 그 밖의 응답은 핸들러가 실행됐다는 뜻이다 —
      // 400(검증 실패)이나 500(내부 오류)도 이미 인가를 통과한 뒤라 문제다.
      if (status != 401 && status != 403) {
        leaked.add("%s %s → %d".formatted(endpoint.method(), endpoint.path(), status));
      }
    }

    assertTrue(
        leaked.isEmpty(),
        "인증 없이 접근 가능한 엔드포인트가 있다. 의도한 공개라면 PUBLIC_PATHS 에 추가할 것:\n  " + String.join("\n  ", leaked));
  }

  @Test
  @DisplayName("관리자 경로는 일반 사용자 인증만으로 접근할 수 없다")
  void adminEndpointsRejectNonAdmin() throws Exception {
    List<String> leaked = new ArrayList<>();

    for (Endpoint endpoint : mappedEndpoints()) {
      if (!endpoint.path().startsWith("/api/v1/admin/") || isPublic(endpoint.path())) {
        continue;
      }

      MvcResult result =
          mockMvc.perform(request(endpoint).with(user("1").roles("USER"))).andReturn();
      int status = result.getResponse().getStatus();

      if (status != 401 && status != 403) {
        leaked.add("%s %s → %d".formatted(endpoint.method(), endpoint.path(), status));
      }
    }

    assertTrue(leaked.isEmpty(), "일반 사용자 권한으로 관리자 경로에 접근할 수 있다:\n  " + String.join("\n  ", leaked));
  }

  // ── 도우미 ────────────────────────────────────────────────────────────────

  private record Endpoint(HttpMethod method, String path) {}

  /**
   * 등록된 모든 핸들러 매핑을 읽는다.
   *
   * <p>경로 변수는 임의 값으로 채운다. 인가는 핸들러 실행 전에 판정되므로 값이 유효할 필요가 없다.
   */
  private List<Endpoint> mappedEndpoints() {
    Set<Endpoint> endpoints = new LinkedHashSet<>();

    handlerMapping
        .getHandlerMethods()
        .forEach(
            (RequestMappingInfo info, HandlerMethod handler) -> {
              Set<String> patterns = info.getPatternValues();
              Set<RequestMethod> methods = info.getMethodsCondition().getMethods();

              for (String pattern : patterns) {
                String path = fillPathVariables(pattern);
                if (methods.isEmpty()) {
                  endpoints.add(new Endpoint(HttpMethod.GET, path));
                  continue;
                }
                for (RequestMethod method : methods) {
                  endpoints.add(new Endpoint(HttpMethod.valueOf(method.name()), path));
                }
              }
            });

    return List.copyOf(endpoints);
  }

  private static String fillPathVariables(String pattern) {
    return pattern.replaceAll("\\{[^/}]+}", "1").replaceAll("/\\*\\*$", "/x");
  }

  private static boolean isPublic(String path) {
    return PUBLIC_PATHS.stream().anyMatch(p -> p.matcher(path).matches());
  }

  private static MockHttpServletRequestBuilder request(Endpoint endpoint) {
    MockHttpServletRequestBuilder builder =
        MockMvcRequestBuilders.request(endpoint.method(), endpoint.path());

    // 본문이 필요한 메서드에는 빈 JSON 을 넣는다. 본문이 없어 415/400 이 먼저 나면
    // 인가가 걸렸는지 아닌지를 구분할 수 없다.
    if (endpoint.method() == HttpMethod.POST
        || endpoint.method() == HttpMethod.PUT
        || endpoint.method() == HttpMethod.PATCH) {
      builder.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    return builder;
  }
}
