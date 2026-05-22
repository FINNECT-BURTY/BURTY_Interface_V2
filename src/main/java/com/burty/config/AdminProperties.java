package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.admin")
public class AdminProperties {

    private String setupKey = "";

    public String getSetupKey() { return setupKey; }
    public void setSetupKey(String setupKey) { this.setupKey = setupKey; }
}
