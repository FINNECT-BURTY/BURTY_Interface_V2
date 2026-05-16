package com.burty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

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
        log.info("BURTY security mode={} resourceServerRequested={}",
                securityProperties.getMode(), resourceServerRequested);

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
                        // refresh 호환 경로: 만료된 access token 으로도 호출 가능해야 함
                        .requestMatchers("/api/v1/sessions/refresh").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll());

        if (resourceServerRequested) {
            log.warn("Resource Server mode requested via burty.security.mode=RESOURCE_SERVER but " +
                    "spring-boot-starter-oauth2-resource-server is not wired. Falling back to JWT_FILTER. " +
                    "Add the dependency and set spring.security.oauth2.resourceserver.jwt.issuer-uri to activate.");
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
