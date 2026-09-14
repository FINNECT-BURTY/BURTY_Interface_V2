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

import com.burty.application.dto.consult.VoiceCapabilityResponse;
import com.burty.application.dto.consult.VoiceSttRequest;
import com.burty.application.dto.consult.VoiceSttResponse;
import com.burty.application.dto.consult.VoiceTtsRequest;
import com.burty.application.dto.consult.VoiceTtsResponse;
import com.burty.application.port.in.consult.VoiceUseCase;
import com.burty.config.VoiceProperties;
import com.burty.core.annotation.CurrentUserId;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController extends BaseController {

  private final VoiceUseCase voiceUseCase;
  private final VoiceProperties voiceProperties;

  /**
   * 음성 기능을 지금 쓸 수 있는지.
   *
   * <p>제공자가 정해지지 않아 기본 설정은 자리표시자를 가리키고 스텁이 켜져 있다. 그 상태에서 화면이 마이크를 내놓으면 사용자는 스텁 문구를 자기 말의 인식 결과로
   * 받아들인다.
   */
  @GetMapping("/capability")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "음성 기능 사용 가능 여부", description = "음성 인식·합성을 실제로 쓸 수 있는지 알려줍니다.")
  public ApiResponse<VoiceCapabilityResponse> capability() {
    boolean available = voiceProperties.isAvailable();
    return ApiResponse.ok(
        new VoiceCapabilityResponse(available, available ? null : "지금은 음성 안내를 사용할 수 없어요"));
  }

  @PostMapping("/stt")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<VoiceSttResponse> stt(
      @CurrentUserId String userId, @Valid @RequestBody VoiceSttRequest request) {
    String audio = request.audioBase64() != null ? request.audioBase64() : "";
    return ApiResponse.ok(new VoiceSttResponse(voiceUseCase.stt(userId, audio)));
  }

  @PostMapping("/tts")
  @AuthLevel(RiskLevel.LEVEL_1)
  public ApiResponse<VoiceTtsResponse> tts(
      @CurrentUserId String userId, @Valid @RequestBody VoiceTtsRequest request) {
    String text = request.text() != null ? request.text() : "";
    return ApiResponse.ok(new VoiceTtsResponse(voiceUseCase.tts(userId, text)));
  }
}
