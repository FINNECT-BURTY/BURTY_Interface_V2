package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.application.service.support.AuditLogger;
import com.burty.domain.admin.model.AuditEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 감사 기록에 개인정보가 평문으로 남지 않는지 확인한다.
 *
 * <p>로그는 {@code %maskedMsg} 로 가리면서 감사 테이블에는 수취 계좌번호가 그대로 저장되고 있었다. 감사 기록은 규제 대응 자료라 보존 기간이 길고, 유출되면
 * 되돌릴 수 없다 — 오히려 더 위험한 쪽이다.
 *
 * <p>호출부에서 가려서 넘기는 것이 원칙이지만, 새 호출부가 생길 때마다 사람이 기억해야 하는 규칙은 언젠가 샌다. 그래서 {@link AuditLogger} 경계에서도 한
 * 번 훑고, 그 동작을 여기 못 박는다.
 */
class AuditLogPiiTests {

  private final List<AuditEvent> saved = new ArrayList<>();
  private final AuditLogger logger =
      new AuditLogger(saved::add, new SingletonProvider<>(new SimpleMeterRegistry()));

  @Test
  @DisplayName("target 의 계좌번호를 가린다")
  void masksAccountNumberInTarget() {
    logger.logSuccess("1", "TRANSFER", "110-234-567890", "amount=10000");

    String target = saved.get(0).target();
    assertFalse(target.contains("110-234-567890"), "계좌번호가 그대로 저장됐다: " + target);
    assertTrue(target.contains("7890"), "식별에 쓸 뒷자리까지 사라졌다: " + target);
  }

  @Test
  @DisplayName("detail 의 주민등록번호·전화번호·이메일을 가린다")
  void masksPersonalIdentifiersInDetail() {
    logger.logFailure(
        "1", "DSR_EXPORT", "USER", "rrn=900101-1234567 phone=010-1234-5678 email=hong@example.com");

    String detail = saved.get(0).detail();
    assertFalse(detail.contains("900101-1234567"), "주민등록번호가 남았다: " + detail);
    assertFalse(detail.contains("010-1234-5678"), "전화번호가 남았다: " + detail);
    assertFalse(detail.contains("hong@example.com"), "이메일이 남았다: " + detail);
  }

  @Test
  @DisplayName("가릴 것이 없는 값은 그대로 둔다")
  void keepsNonSensitiveValues() {
    logger.logSuccess("1", "UPDATE_LIMIT", "LIMIT", "newLimit=3000000");

    AuditEvent event = saved.get(0);
    assertNotNull(event.detail());
    // 금액·설정값까지 가리면 감사 기록이 쓸모없어진다.
    assertTrue(event.detail().contains("3000000"), "가릴 필요 없는 값이 가려졌다: " + event.detail());
    assertTrue(event.target().contains("LIMIT"));
  }

  /** {@code ObjectProvider} 중 이 테스트가 쓰는 메서드만 구현한 최소 구현. */
  private record SingletonProvider<T>(T instance) implements ObjectProvider<T> {

    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }
  }
}
