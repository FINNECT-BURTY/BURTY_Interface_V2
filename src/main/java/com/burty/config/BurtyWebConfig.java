package com.burty.config;

import com.burty.core.constants.CommonConstants;
import com.burty.security.AuthLevelInterceptor;
import com.burty.security.ResourceOwnershipInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class BurtyWebConfig implements WebMvcConfigurer {

    private final AuthLevelInterceptor authLevelInterceptor;
    private final ResourceOwnershipInterceptor resourceOwnershipInterceptor;

    /**
     * com.burty.adapter.in.web 패키지의 모든 @RestController 에 /api/v1 prefix 를 자동 prepend.
     * springdoc / health 등 다른 패키지 컨트롤러는 영향 받지 않음.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                CommonConstants.API_BASE_PATH,
                c -> c.isAnnotationPresent(RestController.class)
                        && c.getPackageName().startsWith("com.burty.adapter.in.web")
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String pattern = CommonConstants.API_BASE_PATH + "/**";
        registry.addInterceptor(authLevelInterceptor)
                .addPathPatterns(pattern)
                .order(0);
        registry.addInterceptor(resourceOwnershipInterceptor)
                .addPathPatterns(pattern)
                .order(1);
    }
}