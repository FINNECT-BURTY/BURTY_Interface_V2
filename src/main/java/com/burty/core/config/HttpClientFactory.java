package com.burty.core.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 연동용 RestTemplate 팩토리.
 *
 * <p>기존 구현은 {@code SimpleClientHttpRequestFactory}(= {@code HttpURLConnection}) 를 썼다. 커넥션 풀이 없어서
 * 요청마다 TCP + TLS 핸드셰이크를 새로 했다. 은행 API 처럼 호출이 잦은 연동에서는 지연과 소켓 고갈의 직접적인 원인이 된다.
 *
 * <p>여기서는 JDK {@link HttpClient} 를 공유한다. HTTP/2 를 지원하고 커넥션을 자동으로 재사용한다. 연동 이름별로 RestTemplate 을 캐시하되
 * <b>타임아웃은 연동마다 다르게</b> 줄 수 있다.
 */
@Component
public class HttpClientFactory {

  private final HttpClient sharedClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .executor(Executors.newVirtualThreadPerTaskExecutor())
          .build();

  private final Map<String, RestTemplate> cache = new ConcurrentHashMap<>();

  /**
   * @param name 연동 식별자 (캐시 키)
   * @param readTimeoutMs 응답 타임아웃. 최소 1초로 보정한다.
   */
  public RestTemplate restTemplate(String name, int readTimeoutMs) {
    int timeout = Math.max(1000, readTimeoutMs);
    return cache.computeIfAbsent(
        name + ":" + timeout,
        key -> {
          JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(sharedClient);
          factory.setReadTimeout(Duration.ofMillis(timeout));
          return new RestTemplate(factory);
        });
  }
}
