package com.burty.config;

import com.burty.util.IpUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * 신뢰 프록시 설정을 {@link IpUtil} 에 적용한다.
 *
 * <p>클라이언트 IP 는 서블릿 필터와 정적 유틸에서 참조하므로 빈 주입이 닿지 않는 곳이 있다. 기동 시 한 번 밀어 넣는다.
 */
@Configuration
public class ClientIpConfiguration {

  private static final Logger log = LoggerFactory.getLogger(ClientIpConfiguration.class);

  private final BurtySecurityProperties securityProperties;

  public ClientIpConfiguration(BurtySecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  @PostConstruct
  void applyTrustedProxies() {
    IpUtil.configure(securityProperties.getTrustedProxies());
    if (securityProperties.getTrustedProxies().isEmpty()) {
      // 로드밸런서 뒤라면 모든 요청의 IP 가 프록시 IP 로 잡힌다. 설정 누락을 알아챌 수 있게 남긴다.
      log.info("신뢰 프록시 미설정 — 전달 헤더를 무시하고 실제 접속 출처만 사용한다.");
    } else {
      log.info("신뢰 프록시 {}개 적용", securityProperties.getTrustedProxies().size());
    }
  }
}
