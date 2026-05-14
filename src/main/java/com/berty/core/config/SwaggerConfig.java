package com.berty.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

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
                .title("BERTY API")
                .description("BERTY REST API 문서")
                .version("1.0.0");

        // SecurityRequirement - 모든 API에 기본 적용 -> 보안 규칙 설정
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(BEARER_SCHEME);

        return new OpenAPI()
                .info(info)
                .components(components)
                .addSecurityItem(securityRequirement);  // 모든 API에 JWT 인증 적용
    }
}
