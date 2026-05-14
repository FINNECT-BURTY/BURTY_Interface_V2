package com.berty.config;

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

    private final com.berty.security.JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BertySecurityProperties securityProperties;

    public SecurityConfig(com.berty.security.JwtAuthenticationFilter jwtAuthenticationFilter,
                          BertySecurityProperties securityProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean resourceServerRequested = securityProperties.isResourceServerEnabled();
        log.info("BERTY security mode={} resourceServerRequested={}",
                securityProperties.getMode(), resourceServerRequested);

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/berty/auth/**").permitAll()
                        .requestMatchers("/api/berty/**").authenticated()
                        .anyRequest().permitAll());

        if (resourceServerRequested) {
            log.warn("Resource Server mode requested via berty.security.mode=RESOURCE_SERVER but " +
                    "spring-boot-starter-oauth2-resource-server is not wired. Falling back to JWT_FILTER. " +
                    "Add the dependency and set spring.security.oauth2.resourceserver.jwt.issuer-uri to activate.");
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
