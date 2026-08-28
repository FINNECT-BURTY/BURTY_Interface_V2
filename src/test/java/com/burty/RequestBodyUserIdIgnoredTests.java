package com.burty;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.dto.auth.LoginRiskEvaluateRequest;
import com.burty.application.dto.cashflow.CashflowScheduleRequest;
import com.burty.application.dto.user.DeviceNameUpdateRequest;
import com.burty.application.dto.user.UserFeedbackRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 요청 본문의 사용자 식별자를 신뢰하지 않는지 검사한다.
 *
 * <p>인가에서 가장 흔한 실수는 인증을 통과시켜 놓고 <b>누구의 자원인지</b>를 요청에서 받는 것이다. 이 프로젝트에서 실제로 네 곳이 그랬다.
 *
 * <ul>
 *   <li>{@code POST /security/login-risk/evaluate} — 남의 기기 등록 여부를 캐고 상대 알림함에 경고를 쌓을 수 있었다
 *   <li>{@code PATCH /devices/{id}/name} — 소유권 검사를 요청 본문의 userId 로 해서 항상 통과했다
 *   <li>{@code POST /cashflow-management/schedules} — 남의 고정 지출 일정을 만들거나 바꿀 수 있었다
 *   <li>{@code POST /feedback} — 남의 이름으로 피드백이 저장됐다
 * </ul>
 *
 * <p>전부 인증은 걸려 있었다. 인증과 인가는 다른 문제다.
 */
@SpringBootTest
class RequestBodyUserIdIgnoredTests extends com.burty.support.IntegrationTestBase {

  /**
   * 사용자 식별자를 본문으로 받아도 되는 예외.
   *
   * <p>{@code TokenIssueRequest} 는 테스트 토큰 발급용이고 운영에서는 {@code ProdStartupValidator} 가 막는다. 소셜·마이데이터
   * 콜백은 인증 전 단계라 토큰이 없다.
   */
  private static final Set<String> ALLOWED_BODY_USER_ID =
      Set.of(
          "TokenIssueRequest",
          "MyDataCallbackRequest",
          "WebAuthnBeginRequest",
          "WebAuthnFinishRequest",
          "IdentityVerificationRequest");

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping handlerMapping;

  @Test
  @DisplayName("본문으로 userId 를 받는 핸들러는 반드시 @CurrentUserId 도 함께 받는다")
  void handlersWithBodyUserIdAlsoTakeAuthenticatedUser() {
    List<String> offenders = new ArrayList<>();

    for (HandlerMethod handler : handlerMapping.getHandlerMethods().values()) {
      Method method = handler.getMethod();

      boolean hasBodyUserId = false;
      boolean hasCurrentUserId = false;

      for (Parameter parameter : method.getParameters()) {
        if (parameter.isAnnotationPresent(com.burty.core.annotation.CurrentUserId.class)) {
          hasCurrentUserId = true;
        }
        if (parameter.isAnnotationPresent(
            org.springframework.web.bind.annotation.RequestBody.class)) {
          Class<?> body = parameter.getType();
          if (ALLOWED_BODY_USER_ID.contains(body.getSimpleName())) {
            continue;
          }
          if (hasUserIdComponent(body)) {
            hasBodyUserId = true;
          }
        }
      }

      if (hasBodyUserId && !hasCurrentUserId) {
        offenders.add(
            "%s.%s".formatted(method.getDeclaringClass().getSimpleName(), method.getName()));
      }
    }

    assertTrue(
        offenders.isEmpty(),
        "요청 본문의 userId 만 보고 동작하는 핸들러가 있다. 인증된 사용자를 함께 받아야 한다:\n  "
            + String.join("\n  ", offenders));
  }

  @Test
  @DisplayName("본문에 userId 가 있어도 서비스는 인증된 사용자로 동작한다")
  void serviceSignaturesTakeAuthenticatedUser() throws Exception {
    // 컨트롤러가 @CurrentUserId 를 받기만 하고 서비스에 넘기지 않으면 아무 의미가 없다.
    // 유스케이스 시그니처가 userId 를 명시적으로 받는지 확인한다.
    record Contract(Class<?> useCase, String method, Class<?>[] params) {}

    List<Contract> contracts =
        List.of(
            new Contract(
                com.burty.application.port.in.auth.LoginRiskUseCase.class,
                "evaluate",
                new Class<?>[] {String.class, LoginRiskEvaluateRequest.class}),
            new Contract(
                com.burty.application.port.in.action.UserFeedbackUseCase.class,
                "submit",
                new Class<?>[] {String.class, UserFeedbackRequest.class}),
            new Contract(
                com.burty.application.port.in.user.DeviceManagementUseCase.class,
                "updateDeviceName",
                new Class<?>[] {String.class, String.class, DeviceNameUpdateRequest.class}),
            new Contract(
                com.burty.application.port.in.cashflow.CashflowManagementUseCase.class,
                "upsertSchedule",
                new Class<?>[] {String.class, CashflowScheduleRequest.class}));

    for (Contract contract : contracts) {
      // 시그니처가 없으면 예외가 난다 — 그 자체가 실패 신호다.
      contract.useCase().getMethod(contract.method(), contract.params());
    }
  }

  private static boolean hasUserIdComponent(Class<?> type) {
    if (type.isRecord()) {
      for (var component : type.getRecordComponents()) {
        if ("userId".equals(component.getName())) return true;
      }
      return false;
    }
    for (var field : type.getDeclaredFields()) {
      if ("userId".equals(field.getName())) return true;
    }
    return false;
  }
}
