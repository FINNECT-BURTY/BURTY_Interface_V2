package com.burty.application.service;

import com.burty.application.port.in.VoiceUseCase;
import com.burty.application.port.out.VoicePort;
import org.springframework.stereotype.Service;

@Service
public class VoiceService implements VoiceUseCase {
    private final VoicePort voicePort;

    public VoiceService(VoicePort voicePort) {
        this.voicePort = voicePort;
    }

    @Override
    public String stt(String userId, String audioBase64) {
        return voicePort.speechToText(audioBase64);
    }

    @Override
    public String tts(String userId, String text) {
        return voicePort.textToSpeech(text);
    }
}
