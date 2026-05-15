package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized security feature toggles.
 *
 * mode = JWT_FILTER (default): use existing self-issued JWT + JwtAuthenticationFilter
 * mode = RESOURCE_SERVER: validate via OAuth2 Resource Server (requires spring-security-oauth2-resource-server dependency
 *                          and `issuer-uri` configured). When enabled, JwtAuthenticationFilter is bypassed.
 */
@Configuration
@ConfigurationProperties(prefix = "burty.security")
public class BurtySecurityProperties {

    private Mode mode = Mode.JWT_FILTER;
    private final ResourceServer resourceServer = new ResourceServer();

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public ResourceServer getResourceServer() { return resourceServer; }

    public boolean isResourceServerEnabled() {
        return mode == Mode.RESOURCE_SERVER && resourceServer.isEnabled();
    }

    public enum Mode { JWT_FILTER, RESOURCE_SERVER }

    public static class ResourceServer {
        private boolean enabled = false;
        private String issuerUri;
        private String jwkSetUri;
        private String audience;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getIssuerUri() { return issuerUri; }
        public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
        public String getJwkSetUri() { return jwkSetUri; }
        public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
    }
}
