package com.berty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "berty.voice")
public class VoiceProperties {
    private boolean stubMode = true;
    private String ttsUrl = "https://api.voice.local/tts";
    private String sttUrl = "https://api.voice.local/stt";
    private String apiKey = "voice-api-key";

    public boolean isStubMode() { return stubMode; }
    public void setStubMode(boolean stubMode) { this.stubMode = stubMode; }
    public String getTtsUrl() { return ttsUrl; }
    public void setTtsUrl(String ttsUrl) { this.ttsUrl = ttsUrl; }
    public String getSttUrl() { return sttUrl; }
    public void setSttUrl(String sttUrl) { this.sttUrl = sttUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
