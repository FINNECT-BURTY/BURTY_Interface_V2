package com.burty.adapter.in.web.mydata;

import com.burty.adapter.in.web.social.OAuthFrontendRedirect;
import com.burty.application.dto.mydata.MyDataLinkResult;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 인가 콜백 결과를 FE 연동 관리 화면으로 돌려보낸다.
 *
 * <p>콜백은 정보제공자가 브라우저를 보내 오는 것이다. 예전에는 JSON 을 돌려줘 브라우저가 그 화면에 멈췄다.
 */
@Component
@RequiredArgsConstructor
public class MyDataLinkRedirect {

  /** 연결 결과를 보여 줄 FE 화면. */
  static final String RESULT_PATH = "/mypage/institutions";

  private final OAuthFrontendRedirect frontendRedirect;

  public ResponseEntity<Void> to(MyDataLinkResult result, HttpServletRequest request) {
    // state 가 유효하지 않으면 묶어 둔 FE 를 알 수 없다. 그때는 브라우저가 온 곳(Referer)을 본다.
    // 모의 동의 화면에서 왔다면 그 FE 로 돌아가고, 실제 정보제공자에서 왔다면 허용 목록에 없어 기본
    // FE 로 간다. 이렇게 하지 않으면 개발 환경에서 만료된 요청이 운영 도메인이나 백엔드 주소로 튄다.
    String origin =
        result.frontendOrigin() != null
            ? result.frontendOrigin()
            : frontendRedirect.resolveFromRequest(request);
    UriComponentsBuilder uri =
        UriComponentsBuilder.fromUriString(frontendRedirect.frontendBase(origin) + RESULT_PATH);
    switch (result.outcome()) {
      case LINKED -> uri.queryParam("linked", result.institutionCode());
      case DENIED ->
          uri.queryParam("link_error", "denied")
              .queryParam("institution", result.institutionCode());
      case EXCHANGE_FAILED ->
          uri.queryParam("link_error", "exchange")
              .queryParam("institution", result.institutionCode());
      case STATE_INVALID -> uri.queryParam("link_error", "state");
    }
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(uri.encode(StandardCharsets.UTF_8).build().toUri())
        .build();
  }
}
