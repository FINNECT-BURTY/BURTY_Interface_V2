/**
 *
 *
 * <pre>
 * <b>Description  : 공통지원 (AuditLogger)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.support
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
package com.burty.application.service.support;

import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.util.PiiMasker;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogger {

  private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

  private final AuditLogPort auditLogPort;
  private final ObjectProvider<MeterRegistry> meterRegistry;

  public void logSuccess(String actorId, String action, String target, String detail) {
    log(actorId, action, target, "SUCCESS", detail);
  }

  public void logFailure(String actorId, String action, String target, String detail) {
    log(actorId, action, target, "FAILURE", detail);
  }

  /**
   * 감사 기록을 남긴다.
   *
   * <p>{@code target} 과 {@code detail} 은 개인정보로 보이는 부분을 가려 저장한다. 로그는 {@code %maskedMsg} 로 가리면서 감사
   * 테이블에는 계좌번호가 평문으로 들어가고 있었다 — 감사 기록은 규제 대응 자료라 보존 기간이 길어 오히려 더 위험하다.
   *
   * <p>호출부에서 이미 가려서 넘기는 것이 원칙이지만, 여기서도 한 번 훑는다. 새 호출부가 생길 때마다 사람이 기억해야 하는 규칙은 언젠가 새는 규칙이다.
   */
  public void log(String actorId, String action, String target, String result, String detail) {
    try {
      auditLogPort.save(
          new AuditEvent(
              UUID.randomUUID().toString(),
              actorId,
              action,
              PiiMasker.scrub(target),
              result,
              PiiMasker.scrub(detail),
              LocalDateTime.now()));
    } catch (Exception e) {
      // 감사 기록 실패로 업무 흐름을 끊지는 않는다. 다만 조용히 넘어가서도 안 된다 —
      // 규제 대응 기록이 사라진 것을 아무도 모르면, 나중에 "기록이 없다" 는 사실만 남는다.
      // burty_audit_save_failed_total 로 알림을 건다.
      countFailure(action);
      log.error(
          "감사 기록 저장 실패 — 규제 기록 유실 actorId={} action={} reason={}",
          actorId,
          action,
          e.getClass().getSimpleName(),
          e);
    }
  }

  private void countFailure(String action) {
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      registry.counter("burty_audit_save_failed_total", "action", action).increment();
    }
  }
}
