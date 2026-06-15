/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (VoiceApiAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.voice
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
package com.burty.adapter.out.voice;

import com.burty.application.port.out.ai.VoicePort;
import com.burty.config.VoiceProperties;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class VoiceApiAdapter implements VoicePort {
  private final RestTemplate restTemplate;
  private final VoiceProperties properties;

  public VoiceApiAdapter(RestTemplate restTemplate, VoiceProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @Override
  public String speechToText(String audioBase64) {
    if (properties.isStubMode()) return "음성 인식 결과(스텁)";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("X-API-KEY", properties.getApiKey());
    Map response =
        restTemplate.postForObject(
            properties.getSttUrl(),
            new HttpEntity<>(Map.of("audio", audioBase64), headers),
            Map.class);
    return response == null ? "" : String.valueOf(response.getOrDefault("text", ""));
  }

  @Override
  public String textToSpeech(String text) {
    if (properties.isStubMode()) return "audio://stub/" + text.hashCode();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("X-API-KEY", properties.getApiKey());
    Map response =
        restTemplate.postForObject(
            properties.getTtsUrl(), new HttpEntity<>(Map.of("text", text), headers), Map.class);
    return response == null ? "" : String.valueOf(response.getOrDefault("audioUrl", ""));
  }
}
