/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (SecurityConfig)</b>
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

import com.burty.core.constant.LogMessages;
import com.burty.core.constants.ApiVersions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  private final com.burty.security.JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AuthRateLimitFilter authRateLimitFilter;
  private final BurtySecurityProperties securityProperties;
  private final BurtyApiProperties apiProperties;
  private final AdminProperties adminProperties;

  public SecurityConfig(
      com.burty.security.JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthRateLimitFilter authRateLimitFilter,
      BurtySecurityProperties securityProperties,
      BurtyApiProperties apiProperties,
      AdminProperties adminProperties) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authRateLimitFilter = authRateLimitFilter;
    this.securityProperties = securityProperties;
    this.apiProperties = apiProperties;
    this.adminProperties = adminProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    boolean resourceServerRequested = securityProperties.isResourceServerEnabled();
    boolean corsEnabled = securityProperties.getCors().isEnabled();
    log.info(
        LogMessages.Security.CONFIG_MODE,
        securityProperties.getMode(),
        resourceServerRequested,
        corsEnabled,
        apiProperties.isSwaggerEnabled());

    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(
                      "/health", "/health/**", "/actuator/health", "/actuator/health/**")
                  .permitAll();
              if (securityProperties.getActuator().isPrometheusPermitAll()) {
                auth.requestMatchers("/actuator/prometheus").permitAll();
              } else {
                auth.requestMatchers("/actuator/prometheus").hasRole("ADMIN");
              }
              auth.requestMatchers("/actuator/**").hasRole("ADMIN");
              if (apiProperties.isSwaggerEnabled()) {
                auth.requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/v1/swagger-ui.html",
                        "/api/v1/swagger-ui/**",
                        "/api/v1/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**")
                    .permitAll();
              }
              auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll();
              auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll();
              auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll();
              auth.requestMatchers("/api/v1/auth/*/authorize-url").permitAll();
              auth.requestMatchers("/api/v1/auth/*/callback").permitAll();
              auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/*/login").permitAll();
              auth.requestMatchers("/api/v1/auth/demo/**").permitAll();
              // 관리자 인증 경로 중 로그인만 공개한다. 예전에는 /admin/auth/** 전체가 permitAll 이라
              // 관리자 계정 생성 엔드포인트(/admin/auth/register)까지 무인증으로 노출돼 있었다.
              // (ADMIN_SETUP_KEY 로 한 겹 막혀 있었지만, 키가 유출되면 곧바로 관리자 계정이 생긴다.)
              auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/login").permitAll();
              auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/refresh").permitAll();
              if (adminProperties.isBootstrapEnabled()) {
                // 최초 관리자 생성 창구. 관리자가 한 명이라도 생기면 서비스 단에서 거절된다.
                log.warn("관리자 부트스트랩 등록이 활성화되어 있습니다 — 운영 환경에서는 반드시 꺼야 합니다.");
                auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/register").permitAll();
              }
              auth.requestMatchers("/api/v1/sessions/refresh").permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/external/openbanking/oauth/callback")
                  .permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/mydata/oauth/callback").permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/mydata/institutions/*/callback")
                  .permitAll();
              // 전사 집계 지표. 컨트롤러 주석에는 "관리자용" 이라 적혀 있었지만 경로가
              // /api/v1/kpi 라 인증만 통과하면 누구나 읽을 수 있었다. @AuthLevel(LEVEL_3) 은
              // 단계 인증이지 권한이 아니다. (@PreAuthorize 는 메서드 보안이 꺼져 있어
              // 붙여도 조용히 무시된다 — 매처로 막는다.)
              auth.requestMatchers(HttpMethod.GET, "/api/v1/kpi/global").hasRole("ADMIN");
              auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
              auth.requestMatchers("/api/v1/**").authenticated();
              auth.requestMatchers(ApiVersions.V2 + "/**").authenticated();
              // 명시적으로 허용한 경로 외에는 전부 차단한다.
              // 예전에는 permitAll() 이라 /api/v1 밖의 모든 경로(오타난 매핑, 새로 추가된
              // 컨트롤러, 서블릿 기본 경로 등)가 인증 없이 열려 있었다. 기본값은 닫혀 있어야 한다.
              auth.anyRequest().denyAll();
            });

    if (corsEnabled) {
      http.cors(Customizer.withDefaults());
    }

    if (resourceServerRequested) {
      log.warn(
          "Resource Server mode requested via burty.security.mode=RESOURCE_SERVER but "
              + "spring-boot-starter-oauth2-resource-server is not wired. Falling back to JWT_FILTER.");
    }
    http.addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    BurtySecurityProperties.Cors cors = securityProperties.getCors();
    if (!cors.isEnabled()) return source;

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(cors.getAllowedOrigins());
    config.setAllowedMethods(cors.getAllowedMethods());
    config.setAllowedHeaders(cors.getAllowedHeaders());
    if (cors.getExposedHeaders() != null && !cors.getExposedHeaders().isEmpty()) {
      config.setExposedHeaders(cors.getExposedHeaders());
    }
    config.setAllowCredentials(cors.isAllowCredentials());
    config.setMaxAge(cors.getMaxAgeSeconds());
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
