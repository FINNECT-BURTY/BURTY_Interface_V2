package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.ai.openai")
public class AiProperties {
    private boolean stubMode = true;
    private String apiKey = "";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private int timeoutMs = 8000;
    private double temperature = 0.2;
    private int maxTokens = 300;

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}
