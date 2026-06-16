/**
 *
 *
 * <pre>
 * <b>Description  : 상담 애플리케이션 서비스 (VoiceService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.consult
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
package com.burty.application.service.consult;

import com.burty.application.port.in.consult.VoiceUseCase;
import com.burty.application.port.out.ai.VoicePort;
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
