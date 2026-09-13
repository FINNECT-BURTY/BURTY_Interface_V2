package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.burty.security.AuthLevel;
import com.burty.security.AuthLevelInterceptor;
import com.burty.security.RiskLevel;
import com.burty.security.RiskProofService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

/**
 * 단계 인증이 모자랄 때의 응답.
 *
 * <p>예전에는 {@code sendError} 로 끝나 봉투도 오류 코드도 없는 403 이 나갔다. FE 는 이것을 로그인 만료와 구분하지 못해 "다시 로그인해주세요" 를
 * 띄웠고, 그 문구가 연동 해제(#147)와 네 기능(#155)의 원인을 오래 가렸다.
 */
class AuthLevelInterceptorTests {

  private static final String USER_ID = "7";

  private RiskProofService riskProofService;
  private AuthLevelInterceptor interceptor;

  /** 단계 인증이 필요한 핸들러. 인터셉터는 애너테이션만 보므로 실제 컨트롤러가 아니어도 된다. */
  static class Guarded {
    @AuthLevel(RiskLevel.LEVEL_2)
    public void level2() {}

    @AuthLevel(RiskLevel.LEVEL_3)
    public void level3() {}

    public void open() {}
  }

  @BeforeEach
  void setUp() {
    riskProofService = mock(RiskProofService.class);
    interceptor = new AuthLevelInterceptor(riskProofService, new ObjectMapper());
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static HandlerMethod handler(String method) throws Exception {
    return new HandlerMethod(new Guarded(), Guarded.class.getMethod(method));
  }

  @Test
  @DisplayName("증명이 없으면 403 과 단계 인증 오류 코드를 봉투에 담아 답한다")
  void missingProofAnswersWithStepUpCode() throws Exception {
    when(riskProofService.verify(any(), anyString(), any())).thenReturn(false);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, handler("level2")));

    assertEquals(403, response.getStatus());
    assertTrue(response.getContentType().startsWith("application/json"));
    String body = response.getContentAsString();
    assertTrue(body.contains("\"errorCode\":\"2006\""), body);
    assertTrue(body.contains("\"success\":false"), body);
  }

  @Test
  @DisplayName("LEVEL_3 도 같은 코드로 답한다 — 어느 등급이 모자란지는 싣지 않는다")
  void level3AnswersWithTheSameCode() throws Exception {
    when(riskProofService.verify(any(), anyString(), any())).thenReturn(false);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, handler("level3")));

    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("\"errorCode\":\"2006\""));
  }

  @Test
  @DisplayName("증명이 맞으면 통과시키고 본문을 쓰지 않는다")
  void validProofPasses() throws Exception {
    when(riskProofService.verify(any(), anyString(), any())).thenReturn(true);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Risk-Proof", "proof");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(request, response, handler("level2")));

    assertEquals(200, response.getStatus());
    assertEquals("", response.getContentAsString());
  }

  @Test
  @DisplayName("등급을 요구하지 않는 핸들러는 증명을 보지 않는다")
  void handlerWithoutAuthLevelIsUntouched() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(new MockHttpServletRequest(), response, handler("open")));

    assertEquals(200, response.getStatus());
  }
}
