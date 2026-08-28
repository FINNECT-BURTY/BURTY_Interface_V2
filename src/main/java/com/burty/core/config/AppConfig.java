/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (AppConfig)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.config
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
package com.burty.core.config;

import com.burty.config.ExternalFinanceProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

  private final ExternalFinanceProperties externalFinanceProperties;
  private final HttpClientFactory httpClientFactory;

  public AppConfig(
      ExternalFinanceProperties externalFinanceProperties, HttpClientFactory httpClientFactory) {
    this.externalFinanceProperties = externalFinanceProperties;
    this.httpClientFactory = httpClientFactory;
  }

  /**
   * 외부 금융 API 기본 RestTemplate.
   *
   * <p>연동별로 타임아웃이 다르면 {@link HttpClientFactory#restTemplate(String, int)} 으로 전용 인스턴스를 받아 쓴다. 예전에는 이
   * 빈 하나를 모든 어댑터가 공유해서 AI/본인확인/음성의 개별 timeout 설정이 전부 무시됐다.
   */
  @Bean
  public RestTemplate restTemplate() {
    return httpClientFactory.restTemplate("external", externalFinanceProperties.getTimeoutMs());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * 시간 소스. 도메인 로직이 {@code LocalDateTime.now()} 를 직접 부르면 (야간 이체 판정, 일일 한도 집계 등) 테스트가 불가능해진다. 모든 시간
   * 의존 로직은 이 빈을 주입받는다.
   */
  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }

  /**
   * Jackson 2 ObjectMapper.
   *
   * <p>HTTP 응답용이 아니다. 아웃박스 페이로드·감사 로그·멱등키 저장처럼 <b>오래 남는 데이터</b>를 직렬화한다. 그래서 설정을 명시적으로 고정한다.
   *
   * <p>{@code new ObjectMapper()} 는 JSR-310 모듈을 등록하지 않아 {@code LocalDate} 직렬화가 아예 깨진다. {@code
   * findAndAddModules()} 로 클래스패스의 모듈을 등록한다.
   *
   * <p>{@code WRITE_DATES_AS_TIMESTAMPS} 는 반드시 꺼야 한다. 켜져 있으면 {@code LocalDate} 가 {@code
   * [2026,8,28]} 배열로 나간다 — 저장된 감사 로그를 사람이 읽을 수 없고, Jackson 의 배열 표현에 데이터가 묶인다. 읽을 때는 배열도 ISO 문자열도 모두
   * 받아들이므로 예전에 저장된 값은 그대로 읽힌다.
   *
   * <p>계약은 {@code ObjectMapperContractTests} 가 검증한다.
   */
  @Bean
  public ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .findAndAddModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        // 응답에 우리가 모르는 필드가 섞여도 역직렬화가 깨지면 안 된다.
        // 여기서 예외가 나면 아웃박스 이벤트가 DEAD 로 격리된다.
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // @JsonView 를 쓰는 순간 뷰에 없는 필드까지 전부 나가는 것을 막는다.
        .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
        .build();
  }
}
