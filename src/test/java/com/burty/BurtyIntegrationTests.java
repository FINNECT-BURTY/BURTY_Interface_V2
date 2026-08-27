/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (BurtyIntegrationTests)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
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
package com.burty;

import com.burty.application.port.in.finance.ExternalFinanceUseCase;
import com.burty.application.port.out.audit.AuditLogPort;
import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.report.MonthlyReportHistoryPort;
import com.burty.domain.admin.entity.AuditLogEntity;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.domain.admin.repository.AuditLogRepository;
import com.burty.domain.consult.entity.MonthlyReportEntity;
import com.burty.domain.consult.repository.MonthlyReportRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.security.JwtTokenProvider;
import com.burty.support.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BurtyIntegrationTests extends IntegrationTestBase {
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private ExternalFinanceUseCase externalFinanceUseCase;
  @Autowired private FamilyAlertPort familyAlertPort;
  @Autowired private MonthlyReportHistoryPort monthlyReportHistoryPort;
  @Autowired private AuditLogPort auditLogPort;
  @Autowired private UserRepository userRepository;
  @Autowired private MonthlyReportRepository monthlyReportRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  private String testUserId;

  @BeforeEach
  void setupUser() {
    String nonce = UUID.randomUUID().toString().replace("-", "");
    UserEntity user = new UserEntity();
    user.setCiHash(nonce + nonce);
    user.setCi("ci-" + nonce);
    user.setPhoneHash("b" + nonce + "b".repeat(31));
    user.setPhone("01000000000");
    user.setStatus(UserEntity.UserStatus.ACTIVE);
    user.setFailedLoginCount(0);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    testUserId = userRepository.save(user).getUserId().toString();
  }

  @Test
  void jwtIssueAndValidate() {
    String token = jwtTokenProvider.generateToken("tester");
    Assertions.assertTrue(jwtTokenProvider.validateToken(token));
    Assertions.assertEquals("tester", jwtTokenProvider.getUserId(token));
  }

  @Test
  void externalAdaptersReturnPayload() {
    var transfer = externalFinanceUseCase.transferToKakaoBank("u1", "3333-22-111", 10000L);
    Assertions.assertEquals("KAKAO_BANK", transfer.provider());
    var pension = externalFinanceUseCase.getPensionSummary("u1");
    Assertions.assertEquals("NATIONAL_PENSION", pension.provider());
  }

  @Test
  void jpaFamilyAlertPortWorks() {
    familyAlertPort.send(testUserId, "alert-message");
    Assertions.assertFalse(familyAlertPort.findByUserId(testUserId).isEmpty());
  }

  @Test
  void jpaMonthlyReportHistoryPortWorks() {
    monthlyReportHistoryPort.saveHistory(testUserId, "2026-04", "SUCCESS", "ok");
    MonthlyReportEntity entity =
        monthlyReportRepository
            .findByUser_UserIdAndPeriodMonth(
                Long.parseLong(testUserId), java.time.YearMonth.parse("2026-04").atDay(1))
            .orElse(null);
    Assertions.assertNotNull(entity);
    Assertions.assertEquals(MonthlyReportEntity.ReportStatus.DELIVERED, entity.getStatus());
  }

  @Test
  void jpaAuditLogPortWorks() {
    long before = auditLogRepository.count();
    auditLogPort.save(
        new AuditEvent(
            UUID.randomUUID().toString(),
            testUserId,
            "TEST_ACTION",
            "TEST_TARGET",
            "SUCCESS",
            "detail",
            LocalDateTime.now()));
    long after = auditLogRepository.count();
    Assertions.assertTrue(after > before);
    AuditLogEntity last = auditLogRepository.findAll().get((int) (after - 1));
    Assertions.assertEquals("TEST_ACTION", last.getAction());
  }
}
