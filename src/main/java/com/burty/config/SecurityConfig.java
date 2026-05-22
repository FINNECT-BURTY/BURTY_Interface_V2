package com.burty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    private final BurtySecurityProperties securityProperties;

    public SecurityConfig(com.burty.security.JwtAuthenticationFilter jwtAuthenticationFilter,
                          BurtySecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean resourceServerRequested = securityProperties.isResourceServerEnabled();
        boolean corsEnabled = securityProperties.getCors().isEnabled();
        log.info("BURTY security mode={} resourceServerRequested={} corsEnabled={}",
                securityProperties.getMode(), resourceServerRequested, corsEnabled);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/swagger-ui.html",
                                "/api/v1/swagger-ui/**",
                                "/api/v1/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/auth/**").permitAll()
                        // refresh 호환 경로: 만료된 access token 으로도 호출 가능해야 함
                        .requestMatchers("/api/v1/sessions/refresh").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll());

        if (corsEnabled) {
            http.cors(Customizer.withDefaults());
        }

        if (resourceServerRequested) {
            log.warn("Resource Server mode requested via burty.security.mode=RESOURCE_SERVER but " +
                    "spring-boot-starter-oauth2-resource-server is not wired. Falling back to JWT_FILTER. " +
                    "Add the dependency and set spring.security.oauth2.resourceserver.jwt.issuer-uri to activate.");
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * burty.security.cors.allowed-origins 가 비어 있으면 빈 source 반환 — Spring Security 는 cors() 비활성과 동일하게 동작.
     * 값이 있을 때만 모든 path 에 대해 해당 origin 들을 허용.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        BurtySecurityProperties.Cors cors = securityProperties.getCors();
        if (!cors.isEnabled()) return source;

        CorsConfiguration config = new CorsConfiguration();
        // allowedOriginPatterns 를 쓰면 와일드카드 + credentials 동시 사용 가능 (allowedOrigins 는 둘 중 하나만 허용).
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
