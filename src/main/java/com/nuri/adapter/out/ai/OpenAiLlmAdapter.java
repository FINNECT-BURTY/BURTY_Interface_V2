package com.nuri.adapter.out.ai;

import com.nuri.application.port.out.LlmPort;
import com.nuri.config.AiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiLlmAdapter implements LlmPort {
    private final RestTemplate restTemplate;
    private final AiProperties properties;

    public OpenAiLlmAdapter(RestTemplate restTemplate, AiProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (properties.isStubMode() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return "자산 흐름은 안정적인 편이에요. 이번 달은 큰 무리 없이 지내셔도 괜찮아요. "
                    + "지금은 자동이체 내역 한 번만 확인해보세요.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            Map response = restTemplate.postForObject(
                    properties.getBaseUrl() + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            return extractText(response);
        } catch (Exception ignored) {
            return "답변을 준비하는 중 문제가 있었어요. 잠시 후 다시 물어봐 주세요. "
                    + "급한 이체는 가족과 한 번 상의하고 진행하세요.";
        }
    }

    private String extractText(Map response) {
        if (response == null) return "";
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object msgObj = firstMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) return "";
        Object content = msgMap.get("content");
        return content == null ? "" : String.valueOf(content);
    }
}
