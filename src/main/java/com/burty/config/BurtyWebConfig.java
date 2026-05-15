package com.burty.config;

import com.burty.security.AuthLevelInterceptor;
import com.burty.security.ResourceOwnershipInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class BurtyWebConfig implements WebMvcConfigurer {

    private final AuthLevelInterceptor authLevelInterceptor;
    private final ResourceOwnershipInterceptor resourceOwnershipInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authLevelInterceptor)
                .addPathPatterns("/api/burty/**")
                .order(0);
        registry.addInterceptor(resourceOwnershipInterceptor)
                .addPathPatterns("/api/burty/**")
                .order(1);
    }
}
