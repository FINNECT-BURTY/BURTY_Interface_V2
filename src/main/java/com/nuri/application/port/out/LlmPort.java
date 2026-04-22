package com.nuri.application.port.out;

public interface LlmPort {
    String generate(String systemPrompt, String userPrompt);
}
