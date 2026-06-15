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

  public SecurityConfig(
      com.burty.security.JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthRateLimitFilter authRateLimitFilter,
      BurtySecurityProperties securityProperties,
      BurtyApiProperties apiProperties) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authRateLimitFilter = authRateLimitFilter;
    this.securityProperties = securityProperties;
    this.apiProperties = apiProperties;
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
              auth.requestMatchers("/actuator/prometheus").permitAll();
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
              auth.requestMatchers("/api/v1/admin/auth/**").permitAll();
              auth.requestMatchers("/api/v1/sessions/refresh").permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/external/openbanking/oauth/callback")
                  .permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/mydata/oauth/callback").permitAll();
              auth.requestMatchers(HttpMethod.GET, "/api/v1/mydata/institutions/*/callback")
                  .permitAll();
              auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
              auth.requestMatchers("/api/v1/**").authenticated();
              auth.anyRequest().permitAll();
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
