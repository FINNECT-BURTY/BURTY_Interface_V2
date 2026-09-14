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

  /** 기본값의 호스트. 실제로 존재하지 않는 주소이며, 제공자가 정해지면 교체해야 한다. */
  private static final String PLACEHOLDER_HOST = "api.voice.local";

  private static final String PLACEHOLDER_API_KEY = "voice-api-key";

  private boolean stubMode = true;
  private String ttsUrl = "https://api.voice.local/tts";
  private String sttUrl = "https://api.voice.local/stt";
  private String apiKey = "voice-api-key";

  /** 음성 API 응답 타임아웃. TTS/STT 는 응답이 느릴 수 있어 금융 API 와 같은 값을 쓰면 안 된다. */
  private int timeoutMs = 15000;

  /**
   * 실제 제공자를 가리키는 설정인가.
   *
   * <p>기본값은 자리표시자다. 그대로 두고 스텁만 끄면 호출은 나가지만 어디에도 닿지 않고, 사용자가 마이크를 누르는 순간에야 실패한다.
   */
  public boolean isConfigured() {
    return isRealUrl(sttUrl) && isRealUrl(ttsUrl) && isRealKey(apiKey);
  }

  /**
   * 사용자에게 음성 기능을 내놓을 수 있는가.
   *
   * <p>스텁 응답은 실제 인식 결과가 아니다. 그것을 화면에 그대로 띄우면 사용자는 자기 말이 잘못 알아들어진 것으로 받아들인다. 제공할 수 없으면 제공한다고 하지 않는
   * 편이 낫다.
   */
  public boolean isAvailable() {
    return !stubMode && isConfigured();
  }

  private static boolean isRealUrl(String url) {
    return url != null && !url.isBlank() && !url.contains(PLACEHOLDER_HOST);
  }

  private static boolean isRealKey(String key) {
    return key != null && !key.isBlank() && !PLACEHOLDER_API_KEY.equals(key.trim());
  }

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
