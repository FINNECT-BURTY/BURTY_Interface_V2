package com.nuri.config;

import com.nuri.security.AuthLevelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class NuriWebConfig implements WebMvcConfigurer {

    private final AuthLevelInterceptor authLevelInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authLevelInterceptor)
                .addPathPatterns("/api/nuri/**");
    }
}
