package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.config.VoiceProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 음성 기능 가용 판정.
 *
 * <p>제공자가 아직 정해지지 않아 기본 설정은 존재하지 않는 주소를 가리키고 스텁이 켜져 있다. 그 상태에서 화면이 마이크를 내놓으면 사용자는 스텁 문구를 자기 말의 인식
 * 결과로 받아들인다 (#129).
 */
class VoiceCapabilityTests {

  private VoiceProperties configured() {
    VoiceProperties properties = new VoiceProperties();
    properties.setSttUrl("https://stt.example.com/v1/recognize");
    properties.setTtsUrl("https://tts.example.com/v1/synthesize");
    properties.setApiKey("real-key");
    return properties;
  }

  @Test
  @DisplayName("기본 설정은 제공할 수 없다고 답한다")
  void defaultsAreNotAvailable() {
    VoiceProperties properties = new VoiceProperties();

    assertTrue(properties.isStubMode());
    assertFalse(properties.isConfigured());
    assertFalse(properties.isAvailable());
  }

  @Test
  @DisplayName("스텁을 꺼도 자리표시자 주소면 제공할 수 없다")
  void placeholderUrlIsNotConfigured() {
    // 호출은 나가지만 어디에도 닿지 않는다. 앱은 뜨고 마이크를 누르는 순간에야 실패한다.
    VoiceProperties properties = new VoiceProperties();
    properties.setStubMode(false);

    assertFalse(properties.isConfigured());
    assertFalse(properties.isAvailable());
  }

  @Test
  @DisplayName("자리표시자 API 키도 설정된 것으로 보지 않는다")
  void placeholderApiKeyIsNotConfigured() {
    VoiceProperties properties = configured();
    properties.setApiKey("voice-api-key");

    assertFalse(properties.isConfigured());
  }

  @Test
  @DisplayName("빈 값은 설정된 것으로 보지 않는다")
  void blankValuesAreNotConfigured() {
    VoiceProperties blankKey = configured();
    blankKey.setApiKey("  ");
    VoiceProperties blankUrl = configured();
    blankUrl.setSttUrl("");

    assertFalse(blankKey.isConfigured());
    assertFalse(blankUrl.isConfigured());
  }

  @Test
  @DisplayName("실제 제공자를 가리키고 스텁을 꺼야 제공할 수 있다")
  void realProviderIsAvailable() {
    VoiceProperties properties = configured();
    properties.setStubMode(false);

    assertTrue(properties.isConfigured());
    assertTrue(properties.isAvailable());
  }

  @Test
  @DisplayName("설정이 갖춰져도 스텁이 켜져 있으면 제공하지 않는다")
  void stubModeIsNeverAvailable() {
    // 스텁 응답은 실제 인식 결과가 아니다. 제공할 수 없으면 제공한다고 하지 않는다.
    VoiceProperties properties = configured();
    properties.setStubMode(true);

    assertTrue(properties.isConfigured());
    assertFalse(properties.isAvailable());
  }
}
