package com.burty.adapter.out.voice;

import com.burty.application.port.out.VoicePort;
import com.burty.config.VoiceProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class VoiceApiAdapter implements VoicePort {
    private final RestTemplate restTemplate;
    private final VoiceProperties properties;

    public VoiceApiAdapter(RestTemplate restTemplate, VoiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String speechToText(String audioBase64) {
        if (properties.isStubMode()) return "음성 인식 결과(스텁)";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", properties.getApiKey());
        Map response = restTemplate.postForObject(properties.getSttUrl(), new HttpEntity<>(Map.of("audio", audioBase64), headers), Map.class);
        return response == null ? "" : String.valueOf(response.getOrDefault("text", ""));
    }

    @Override
    public String textToSpeech(String text) {
        if (properties.isStubMode()) return "audio://stub/" + text.hashCode();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-API-KEY", properties.getApiKey());
        Map response = restTemplate.postForObject(properties.getTtsUrl(), new HttpEntity<>(Map.of("text", text), headers), Map.class);
        return response == null ? "" : String.valueOf(response.getOrDefault("audioUrl", ""));
    }
}
