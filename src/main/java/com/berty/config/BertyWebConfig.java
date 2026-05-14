package com.berty.config;

import com.berty.security.AuthLevelInterceptor;
import com.berty.security.ResourceOwnershipInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class BertyWebConfig implements WebMvcConfigurer {

    private final AuthLevelInterceptor authLevelInterceptor;
    private final ResourceOwnershipInterceptor resourceOwnershipInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authLevelInterceptor)
                .addPathPatterns("/api/berty/**")
                .order(0);
        registry.addInterceptor(resourceOwnershipInterceptor)
                .addPathPatterns("/api/berty/**")
                .order(1);
    }
}
