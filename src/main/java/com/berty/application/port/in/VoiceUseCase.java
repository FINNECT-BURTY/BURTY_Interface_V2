package com.berty.application.port.in;

public interface VoiceUseCase {
    String stt(String userId, String audioBase64);
    String tts(String userId, String text);
}
