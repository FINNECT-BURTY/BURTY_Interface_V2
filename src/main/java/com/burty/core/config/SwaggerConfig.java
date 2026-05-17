package com.burty.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    private final String baseUrl;

    public SwaggerConfig(@Value("${app.base-url:}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        // 보안 스키마 정의
        Components components = new Components()
                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("JWT 토큰을 입력하세요. Bearer 접두사 없이 토큰만 입력하세요."));

        // Info 메타데이터
        Info info = new Info()
                .title("BURTY API")
                .description("BURTY REST API 문서")
                .version("1.0.0");

        // SecurityRequirement - 모든 API에 기본 적용 -> 보안 규칙 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_SCHEME);

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .components(components)
                .addSecurityItem(securityRequirement);

        // app.base-url 을 servers[0] 으로 명시 — nginx 뒤의 https 스킴이 누락되지 않도록.
        // 미설정이면 Springdoc 의 기본 추론(요청 헤더 기반)을 그대로 사용.
        if (baseUrl != null && !baseUrl.isBlank()) {
            openAPI.addServersItem(new Server().url(baseUrl).description("Configured base URL"));
        }

        return openAPI;
    }
}
