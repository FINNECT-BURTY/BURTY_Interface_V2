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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
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
   * <p>{@code new ObjectMapper()} 는 JSR-310 등 모듈을 등록하지 않아 {@code LocalDate}/{@code LocalDateTime}
   * 직렬화가 깨진다. Spring 빌더를 쓰면 클래스패스에 있는 모듈이 자동 등록된다.
   */
  @Bean
  public ObjectMapper objectMapper() {
    return Jackson2ObjectMapperBuilder.json().build();
  }
}
