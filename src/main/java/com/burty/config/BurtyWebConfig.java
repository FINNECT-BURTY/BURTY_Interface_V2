/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (BurtyWebConfig)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import com.burty.adapter.in.web.health.HealthController;
import com.burty.core.constants.ApiVersions;
import com.burty.core.constants.CommonConstants;
import com.burty.security.AuthLevelInterceptor;
import com.burty.security.RequestBodyOwnershipInterceptor;
import com.burty.security.ResourceOwnershipInterceptor;
import com.burty.security.resolver.CurrentUserIdArgumentResolver;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class BurtyWebConfig implements WebMvcConfigurer {

  private final AuthLevelInterceptor authLevelInterceptor;
  private final ResourceOwnershipInterceptor resourceOwnershipInterceptor;
  private final RequestBodyOwnershipInterceptor requestBodyOwnershipInterceptor;
  private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

  private static final String V1_PACKAGE = "com.burty.adapter.in.web";
  private static final String V2_PACKAGE = "com.burty.adapter.in.web.v2";

  /**
   * 버전 접두사를 붙이지 않는 컨트롤러.
   *
   * <p>{@code /health} 는 API 가 아니라 운영 약속이다. 컨테이너 HEALTHCHECK, compose, nginx, Jenkins 배포 확인이 모두 이
   * 경로를 부르고, {@code SecurityConfig} 도 이 경로를 무인증으로 연다. 접두사가 붙어 {@code /api/v1/health} 로 옮겨간 뒤로 운영 쪽
   * 헬스체크가 전부 실패했다.
   */
  private static final Set<Class<?>> UNVERSIONED = Set.of(HealthController.class);

  /**
   * 패키지로 API 버전을 결정한다.
   *
   * <pre>
   *   com.burty.adapter.in.web.v2.**  → /api/v2
   *   com.burty.adapter.in.web.**     → /api/v1
   * </pre>
   *
   * <p>예전에는 {@code /api/v1} 하나만 붙일 수 있어서 v2 를 도입할 경로가 없었다. 두 버전을 동시에 서비스할 수 없으면 breaking change 를 못
   * 하고 v1 에 필드를 계속 덧붙이게 된다. v2 컨트롤러는 {@code adapter.in.web.v2} 패키지에 추가하기만 하면 되고, v1 은 폐기 기한까지 그대로
   * 병행 운영한다.
   *
   * <p>더 구체적인 규칙(v2)을 먼저 등록해야 한다. 순서가 바뀌면 v2 컨트롤러도 v1 prefix 를 받는다.
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        ApiVersions.V2,
        c ->
            c.isAnnotationPresent(RestController.class)
                && c.getPackageName().startsWith(V2_PACKAGE));
    configurer.addPathPrefix(
        CommonConstants.API_BASE_PATH,
        c ->
            c.isAnnotationPresent(RestController.class)
                && c.getPackageName().startsWith(V1_PACKAGE)
                && !c.getPackageName().startsWith(V2_PACKAGE)
                && !UNVERSIONED.contains(c));
  }

  /**
   * {@code @CurrentUserId} 파라미터 리졸버 등록.
   *
   * <p>인터셉터 기반 IDOR 방어는 파라미터 이름 관례에 의존해 취약했다. 사용자 ID 를 클라이언트 입력이 아니라 인증 컨텍스트에서만 얻게 만드는 쪽이 근본 해결이다.
   * 인터셉터는 아직 남아 있는 {@code @RequestParam userId} 엔드포인트를 위한 안전망으로 유지한다.
   */
  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserIdArgumentResolver);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    String pattern = CommonConstants.API_BASE_PATH + "/**";
    registry.addInterceptor(authLevelInterceptor).addPathPatterns(pattern).order(0);
    registry.addInterceptor(resourceOwnershipInterceptor).addPathPatterns(pattern).order(1);
    registry.addInterceptor(requestBodyOwnershipInterceptor).addPathPatterns(pattern).order(2);
  }
}
