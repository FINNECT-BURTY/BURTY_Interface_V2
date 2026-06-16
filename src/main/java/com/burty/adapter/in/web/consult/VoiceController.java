/**
 *
 *
 * <pre>
 * <b>Description  : 상담 API 컨트롤러 (VoiceController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.consult
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.adapter.in.web.consult;

import com.burty.application.dto.consult.VoiceSttRequest;
import com.burty.application.dto.consult.VoiceSttResponse;
import com.burty.application.dto.consult.VoiceTtsRequest;
import com.burty.application.dto.consult.VoiceTtsResponse;
import com.burty.application.port.in.consult.VoiceUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController extends BaseController {

  private final VoiceUseCase voiceUseCase;

  @PostMapping("/stt")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<VoiceSttResponse> stt(@RequestBody VoiceSttRequest request) {
    String userId = request.userId() != null ? request.userId() : "";
    String audio = request.audioBase64() != null ? request.audioBase64() : "";
    return ApiResponse.ok(new VoiceSttResponse(voiceUseCase.stt(userId, audio)));
  }

  @PostMapping("/tts")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<VoiceTtsResponse> tts(@RequestBody VoiceTtsRequest request) {
    String userId = request.userId() != null ? request.userId() : "";
    String text = request.text() != null ? request.text() : "";
    return ApiResponse.ok(new VoiceTtsResponse(voiceUseCase.tts(userId, text)));
  }
}
