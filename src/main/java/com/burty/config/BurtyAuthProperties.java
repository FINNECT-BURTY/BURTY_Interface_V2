package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 인증 관련 플래그. 운영에서는 테스트용 JWT 발급을 끄는 것이 기본입니다.
 */
@Component
@ConfigurationProperties(prefix = "burty.auth")
public class BurtyAuthProperties {

    /**
     * {@code POST /api/burty/auth/token} 허용 여부. {@code spring.profiles.active}에 prod가 있으면 무조건 비활성화됩니다.
     */
    private boolean testTokenEnabled = false;

    public boolean isTestTokenEnabled() {
        return testTokenEnabled;
    }

    public void setTestTokenEnabled(boolean testTokenEnabled) {
        this.testTokenEnabled = testTokenEnabled;
    }
}
