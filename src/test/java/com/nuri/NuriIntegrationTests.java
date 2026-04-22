package com.nuri;

import com.nuri.application.port.in.ExternalFinanceUseCase;
import com.nuri.application.port.out.AuditLogPort;
import com.nuri.application.port.out.FamilyAlertPort;
import com.nuri.application.port.out.MonthlyReportHistoryPort;
import com.nuri.domain.entity.AuditLogEntity;
import com.nuri.domain.entity.MonthlyReportEntity;
import com.nuri.domain.entity.UserEntity;
import com.nuri.domain.model.AuditEvent;
import com.nuri.domain.repository.AuditLogRepository;
import com.nuri.domain.repository.MonthlyReportRepository;
import com.nuri.domain.repository.UserRepository;
import com.nuri.security.JwtTokenProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
class NuriIntegrationTests {
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private ExternalFinanceUseCase externalFinanceUseCase;
    @Autowired
    private FamilyAlertPort familyAlertPort;
    @Autowired
    private MonthlyReportHistoryPort monthlyReportHistoryPort;
    @Autowired
    private AuditLogPort auditLogPort;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MonthlyReportRepository monthlyReportRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private final String testUserId = "11111111-1111-1111-1111-111111111111";

    @BeforeEach
    void setupUser() {
        UUID uuid = UUID.fromString(testUserId);
        if (userRepository.findById(uuid).isEmpty()) {
            UserEntity user = new UserEntity();
            user.setUserId(uuid);
            user.setCiHash("a".repeat(64));
            user.setCiEncrypted("enc-ci".getBytes());
            user.setPhoneHash("b".repeat(64));
            user.setPhoneEncrypted("enc-phone".getBytes());
            user.setStatus(UserEntity.UserStatus.ACTIVE);
            user.setFailedLoginCount(0);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Test
    void jwtIssueAndValidate() {
        String token = jwtTokenProvider.generateToken("tester");
        Assertions.assertTrue(jwtTokenProvider.validateToken(token));
        Assertions.assertEquals("tester", jwtTokenProvider.getUserId(token));
    }

    @Test
    void externalAdaptersReturnPayload() {
        Map<String, Object> transfer = externalFinanceUseCase.transferToKakaoBank("u1", "3333-22-111", 10000L);
        Assertions.assertEquals("KAKAO_BANK", transfer.get("provider"));
        Map<String, Object> pension = externalFinanceUseCase.getPensionSummary("u1");
        Assertions.assertEquals("NATIONAL_PENSION", pension.get("provider"));
    }

    @Test
    void jpaFamilyAlertPortWorks() {
        familyAlertPort.send(testUserId, "alert-message");
        Assertions.assertFalse(familyAlertPort.findByUserId(testUserId).isEmpty());
    }

    @Test
    void jpaMonthlyReportHistoryPortWorks() {
        monthlyReportHistoryPort.saveHistory(testUserId, "2026-04", "SUCCESS", "ok");
        MonthlyReportEntity entity = monthlyReportRepository
                .findByUser_UserIdAndPeriodMonth(UUID.fromString(testUserId), java.time.YearMonth.parse("2026-04").atDay(1))
                .orElse(null);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(MonthlyReportEntity.ReportStatus.DELIVERED, entity.getStatus());
    }

    @Test
    void jpaAuditLogPortWorks() {
        long before = auditLogRepository.count();
        auditLogPort.save(new AuditEvent(UUID.randomUUID().toString(), testUserId, "TEST_ACTION", "TEST_TARGET", "SUCCESS", "detail", LocalDateTime.now()));
        long after = auditLogRepository.count();
        Assertions.assertTrue(after > before);
        AuditLogEntity last = auditLogRepository.findAll().get((int) (after - 1));
        Assertions.assertEquals("TEST_ACTION", last.getAction());
    }
}
