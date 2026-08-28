package com.burty.config;

import com.burty.util.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * {@code server.forward-headers-strategy=framework} 일 때의 전달 헤더 처리.
 *
 * <p>스프링의 {@link ForwardedHeaderFilter} 는 {@code X-Forwarded-Proto/Host} 를 반영해 https 스킴을 살려준다.
 * Swagger 의 OpenAPI servers 와 소셜 로그인 리다이렉트 검증이 여기에 의존한다. 그래서 끌 수 없다.
 *
 * <p>문제는 이 필터가 {@code X-Forwarded-For} 로 {@code remoteAddr} 까지 덮어쓴다는 점이다. 그러면 "실제 접속 출처" 라는 것이
 * 사라지고, 신뢰 프록시 판정의 기준점이 없어진다 — 클라이언트가 보낸 값을 클라이언트가 보낸 값으로 검증하는 꼴이 된다.
 *
 * <p>그래서 덮어쓰기 <b>전에</b> 진짜 peer 주소를 요청 속성에 남긴다. {@link ClientIpResolver} 는 이 값을 신뢰 판정의 기준으로 쓴다.
 *
 * <p>스프링 부트는 이 필터 빈이 있으면 자동 등록에서 물러난다({@code @ConditionalOnMissingFilterBean}).
 */
@Configuration
@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "framework")
public class ForwardedHeaderConfiguration {

  @Bean
  public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
    ForwardedHeaderFilter filter =
        new ForwardedHeaderFilter() {
          @Override
          protected void doFilterInternal(
              HttpServletRequest request, HttpServletResponse response, FilterChain chain)
              throws ServletException, IOException {
            request.setAttribute(ClientIpResolver.PEER_ADDRESS_ATTRIBUTE, request.getRemoteAddr());
            super.doFilterInternal(request, response, chain);
          }
        };
    FilterRegistrationBean<ForwardedHeaderFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
