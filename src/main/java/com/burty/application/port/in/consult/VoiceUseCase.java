/**
 *
 *
 * <pre>
 * <b>Description  : 상담 유스케이스 포트 (VoiceUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.consult
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
package com.burty.application.port.in.consult;

public interface VoiceUseCase {
  String stt(String userId, String audioBase64);

  String tts(String userId, String text);
}
