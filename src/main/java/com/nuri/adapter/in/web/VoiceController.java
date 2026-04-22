package com.nuri.adapter.in.web;

import com.nuri.adapter.in.web.dto.VoiceSttRequest;
import com.nuri.adapter.in.web.dto.VoiceTtsRequest;
import com.nuri.core.dto.response.ApiResponse;
import com.nuri.application.port.in.VoiceUseCase;
import com.nuri.security.AuthLevel;
import com.nuri.security.RiskLevel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nuri/voice")
public class VoiceController {
    private final VoiceUseCase voiceUseCase;

    public VoiceController(VoiceUseCase voiceUseCase) {
        this.voiceUseCase = voiceUseCase;
    }

    @PostMapping("/stt")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, String>> stt(@RequestBody VoiceSttRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "";
        String audio = request.getAudioBase64() != null ? request.getAudioBase64() : "";
        return ApiResponse.ok(Map.of("text", voiceUseCase.stt(userId, audio)));
    }

    @PostMapping("/tts")
    @AuthLevel(RiskLevel.LEVEL_1)
    public ApiResponse<Map<String, String>> tts(@RequestBody VoiceTtsRequest request) {
        String userId = request.getUserId() != null ? request.getUserId() : "";
        String text = request.getText() != null ? request.getText() : "";
        return ApiResponse.ok(Map.of("audioUrl", voiceUseCase.tts(userId, text)));
    }
}
