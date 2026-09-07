package com.burty.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * {@code burty.webauthn.origin} 과 {@code burty.webauthn.rp-id} 가 서로 맞는지 기동 시점에 확인한다.
 *
 * <p>{@code origin} 은 브라우저가 {@code clientDataJSON} 에 넣는 값과 비교된다. 그 값은 <b>패스키를 호출한 웹페이지의 origin</b>,
 * 즉 프론트엔드다. 백엔드 API 의 주소가 아니다.
 *
 * <p>예전에는 세 프로파일 모두 {@code burty.webauthn.origin=${app.base-url}} 이었다. prod 는 FE 와 BE 가 같은 도메인이라
 * 우연히 맞아떨어졌고, dev·staging 은 스텁이 서명을 검증하지 않아 불일치가 드러나지 않았다. <b>설정이 틀렸는데도 아무것도 깨지지 않는 상태</b>였고, FE 를
 * 별도 호스트로 옮기는 순간 이체가 전부 막힐 참이었다.
 *
 * <p>그래서 전제를 기동 시점에 확인한다. WebAuthn 규격상 {@code rpId} 는 호출한 페이지 origin 의 호스트이거나 그 상위 도메인이어야 하므로, 둘의
 * 관계만 봐도 설정이 어긋난 것을 잡을 수 있다.
 */
@Configuration
public class WebAuthnOriginValidator {

  private static final Logger log = LoggerFactory.getLogger(WebAuthnOriginValidator.class);

  private final WebAuthnProperties properties;

  public WebAuthnOriginValidator(WebAuthnProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void verify() {
    String origin = properties.getOrigin();
    String rpId = properties.getRpId();

    String host = hostOf(origin);
    if (host == null) {
      log.error("burty.webauthn.origin 을 URL 로 읽을 수 없다 — {}. 스킴을 포함한 origin 이어야 한다.", origin);
      return;
    }

    if (matchesRpId(host, rpId)) {
      log.info("WebAuthn origin 확인 — origin {} / rpId {}", origin, rpId);
      return;
    }

    // 죽이지는 않는다. 로컬에서 포트만 바꿔 띄우는 경우까지 막으면 실효보다 불편이 크다.
    // 대신 놓칠 수 없게 남긴다 — 이 불일치는 이체 시점에야 "인증 실패" 로만 드러난다.
    log.error(
        "WebAuthn 설정 불일치 — origin 은 {} 인데 rpId 는 {} 다. "
            + "rpId 는 origin 의 호스트({})이거나 그 상위 도메인이어야 한다. "
            + "origin 에 백엔드 API 주소를 넣지 않았는지 확인할 것 — 이 값은 프론트엔드 origin 이다.",
        origin,
        rpId,
        host);
  }

  private String hostOf(String origin) {
    if (origin == null || origin.isBlank()) return null;
    try {
      return URI.create(origin).getHost();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /** 호스트가 rpId 와 같거나 그 하위 도메인인가. WebAuthn 이 요구하는 관계다. */
  private boolean matchesRpId(String host, String rpId) {
    if (rpId == null || rpId.isBlank()) return false;
    return host.equals(rpId) || host.endsWith("." + rpId);
  }
}
