/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (VoiceProperties)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "burty.voice")
public class VoiceProperties {
  private boolean stubMode = true;
  private String ttsUrl = "https://api.voice.local/tts";
  private String sttUrl = "https://api.voice.local/stt";
  private String apiKey = "voice-api-key";

  /** 음성 API 응답 타임아웃. TTS/STT 는 응답이 느릴 수 있어 금융 API 와 같은 값을 쓰면 안 된다. */
  private int timeoutMs = 15000;

  public boolean isStubMode() {
    return stubMode;
  }

  public void setStubMode(boolean stubMode) {
    this.stubMode = stubMode;
  }

  public String getTtsUrl() {
    return ttsUrl;
  }

  public void setTtsUrl(String ttsUrl) {
    this.ttsUrl = ttsUrl;
  }

  public String getSttUrl() {
    return sttUrl;
  }

  public void setSttUrl(String sttUrl) {
    this.sttUrl = sttUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(int timeoutMs) {
    this.timeoutMs = timeoutMs;
  }
}
