/**
 *
 *
 * <pre>
 * <b>Description  : 소셜로그인 (OAuthHttpClient)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.social
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
package com.burty.adapter.out.social;

import com.burty.config.SocialLoginProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.auth.model.SocialProvider;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * OAuth provider HTTP 호출 전용 wrapper.
 *
 * <p>Why: token endpoint / userinfo endpoint 호출의 try/catch + onStatus + timeout 보일러플레이트가 provider
 * 마다 동일하게 반복됨. provider strategy 들이 비즈니스 로직(응답 파싱)에만 집중하도록 분리.
 */
@Component
public class OAuthHttpClient {
  private static final Logger log = LoggerFactory.getLogger(OAuthHttpClient.class);
  private static final int LOG_BODY_LIMIT = 500;

  private final WebClient webClient;
  private final Duration timeout;

  public OAuthHttpClient(SocialLoginProperties properties) {
    this.webClient = WebClient.create();
    this.timeout = Duration.ofMillis(properties.getTimeoutMs());
  }

  /** application/x-www-form-urlencoded POST. token endpoint 호출용. */
  @SuppressWarnings("unchecked")
  public Map<String, Object> postForm(
      SocialProvider provider,
      String uri,
      MultiValueMap<String, String> form,
      String operationLabel) {
    return execute(
        provider,
        operationLabel,
        () ->
            webClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(HttpStatusCode::isError, errorHandler(provider, operationLabel))
                .bodyToMono(Map.class)
                .block(timeout));
  }

  /** Bearer GET. userinfo endpoint 호출용. */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getJson(
      SocialProvider provider, String uri, String bearerToken, String operationLabel) {
    return execute(
        provider,
        operationLabel,
        () ->
            webClient
                .get()
                .uri(uri)
                .headers(h -> h.setBearerAuth(bearerToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, errorHandler(provider, operationLabel))
                .bodyToMono(Map.class)
                .block(timeout));
  }

  private Map<String, Object> execute(
      SocialProvider provider,
      String operationLabel,
      java.util.function.Supplier<Map<String, Object>> call) {
    Map<String, Object> response;
    try {
      response = call.get();
    } catch (BusinessException be) {
      throw be;
    } catch (WebClientResponseException wcre) {
      log.warn(
          "OAuth {} webclient error provider={} status={} body={}",
          operationLabel,
          provider,
          wcre.getStatusCode().value(),
          truncate(wcre.getResponseBodyAsString()));
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR, provider + " " + operationLabel + " 응답 오류");
    } catch (Exception e) {
      log.warn(
          "OAuth {} request failed provider={} reason={}",
          operationLabel,
          provider,
          e.getClass().getSimpleName(),
          e);
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR, provider + " " + operationLabel + " 요청에 실패했습니다.");
    }
    if (response == null) {
      throw new BusinessException(
          ErrorCode.EXTERNAL_API_ERROR, provider + " " + operationLabel + " 응답이 비어 있습니다.");
    }
    return response;
  }

  private java.util.function.Function<
          org.springframework.web.reactive.function.client.ClientResponse,
          Mono<? extends Throwable>>
      errorHandler(SocialProvider provider, String operationLabel) {
    return resp ->
        resp.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(
                body -> {
                  log.warn(
                      "OAuth {} endpoint error provider={} status={} body={}",
                      operationLabel,
                      provider,
                      resp.statusCode().value(),
                      truncate(body));
                  return Mono.error(
                      new BusinessException(
                          ErrorCode.EXTERNAL_API_ERROR,
                          provider
                              + " "
                              + operationLabel
                              + " 실패 (status="
                              + resp.statusCode().value()
                              + ")"));
                });
  }

  private static String truncate(String s) {
    if (s == null) return "";
    return s.length() <= LOG_BODY_LIMIT ? s : s.substring(0, LOG_BODY_LIMIT) + "...(truncated)";
  }
}
