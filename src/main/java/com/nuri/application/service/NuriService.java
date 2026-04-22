package com.nuri.application.service;

import com.nuri.core.exception.BusinessException;
import com.nuri.core.error.enums.ErrorCode;
import com.nuri.application.port.in.NuriUseCase;
import com.nuri.application.port.out.*;
import com.nuri.domain.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NuriService implements NuriUseCase {
    private static final long FAMILY_ALERT_THRESHOLD = 1_000_000L;
    private static final long LARGE_TRANSFER_THRESHOLD = 3_000_000L;
    private final EasyReadPort easyReadPort;
    private final MyDataPort myDataPort;
    private final BiometricAuthPort biometricAuthPort;
    private final FamilyAlertPort familyAlertPort;
    private final AuditLogPort auditLogPort;
    private final Map<String, List<FamilyConsent>> familyConsentStore = new ConcurrentHashMap<>();
    private final Map<String, List<String>> registeredAccountStore = new ConcurrentHashMap<>();
    private final Map<String, Long> userLimitStore = new ConcurrentHashMap<>();
    private final Map<String, TransferResult> transferStore = new ConcurrentHashMap<>();
    private final Map<String, List<TransferResult>> transferHistoryByUser = new ConcurrentHashMap<>();
    private final Map<String, Boolean> linkedInstitutionStore = new ConcurrentHashMap<>();

    public NuriService(EasyReadPort easyReadPort, MyDataPort myDataPort, BiometricAuthPort biometricAuthPort, FamilyAlertPort familyAlertPort,
                             AuditLogPort auditLogPort) {
        this.easyReadPort = easyReadPort;
        this.myDataPort = myDataPort;
        this.biometricAuthPort = biometricAuthPort;
        this.familyAlertPort = familyAlertPort;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public ConsultationResult consult(String userId, String question) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        String raw = "지난달 변동성은 %.1f%% 입니다. 이번 달 지출은 %.0f원이며 전체 자산은 %.0f원입니다. 급격한 이동보다 6개월 단위로 점검하세요."
                .formatted(snapshot.getVolatilityPercent(), snapshot.getMonthlySpend(), snapshot.getTotalAsset());
        return new ConsultationResult(
                easyReadPort.toEasyRead(raw),
                easyReadPort.toSignalColor(snapshot.getVolatilityPercent()),
                List.of("만기 자산 재배치 상담", "가족 알림 점검", "질문: " + question)
        );
    }

    @Override
    public MonthlyReport createMonthlyReport(String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        String summary = easyReadPort.toEasyRead("포트폴리오 변동성은 %.1f%% 입니다. 이번 달 지출은 %.0f원입니다. 다음 달 예금 만기를 점검하세요."
                .formatted(snapshot.getVolatilityPercent(), snapshot.getMonthlySpend()));
        return new MonthlyReport(userId, YearMonth.now().toString(), summary, easyReadPort.toSignalColor(snapshot.getVolatilityPercent()),
                "만기 예금 재배치", List.of("생활비 6개월 버퍼 유지", "고위험 자산 비중 관리", "가족 알림 유지"));
    }

    @Override
    public TransferResult transfer(String userId, String fromAccount, String toAccount, long amount, String description, String assertionToken) {
        if (!biometricAuthPort.verifyAssertion(userId, assertionToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "FIDO2/WebAuthn 인증 검증에 실패했습니다.");
        }

        boolean notify = amount >= FAMILY_ALERT_THRESHOLD;
        boolean unusualNightTransfer = LocalTime.now().isAfter(LocalTime.of(23, 0)) || LocalTime.now().isBefore(LocalTime.of(6, 0));
        boolean unregisteredAccountTransfer = registeredAccountStore.getOrDefault(userId, List.of()).stream().noneMatch(toAccount::equals);
        boolean largeTransfer = amount >= LARGE_TRANSFER_THRESHOLD;

        if (unusualNightTransfer || unregisteredAccountTransfer || largeTransfer) {
            familyAlertPort.send(userId, "[경고] 이상거래 의심: 심야/미등록계좌/대규모 이체 패턴 감지");
            notify = true;
        }
        if (notify) {
            familyAlertPort.send(userId, "부모님 계정에서 %,d원이 %s 계좌로 이체되었습니다.".formatted(amount, toAccount));
        }
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "TRANSFER", toAccount, "SUCCESS",
                "amount=" + amount + ", description=" + description, LocalDateTime.now()
        ));
        TransferResult result = new TransferResult(UUID.randomUUID().toString(), "COMPLETED", notify);
        transferStore.put(result.getTransferId(), result);
        transferHistoryByUser.computeIfAbsent(userId, key -> new ArrayList<>()).add(result);
        return result;
    }

    @Override
    public List<FamilyAlert> getFamilyAlerts(String userId) {
        return familyAlertPort.findByUserId(userId);
    }

    @Override
    public void updateLimit(String userId, long newLimit) {
        if (newLimit < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "한도는 0 이상이어야 합니다.");
        }
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "UPDATE_LIMIT", "LIMIT", "SUCCESS",
                "newLimit=" + newLimit, LocalDateTime.now()
        ));
        userLimitStore.put(userId, newLimit);
    }

    @Override
    public long getLimit(String userId) {
        return userLimitStore.getOrDefault(userId, 0L);
    }

    @Override
    public void registerFamilyConsent(String parentUserId, String childUserId) {
        familyConsentStore.compute(parentUserId, (k, v) -> {
            List<FamilyConsent> consents = v == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(v);
            consents.add(new FamilyConsent(parentUserId, childUserId, true));
            return consents;
        });
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), parentUserId, "REGISTER_FAMILY_CONSENT", childUserId, "SUCCESS",
                "consent=true", LocalDateTime.now()
        ));
    }

    @Override
    public boolean updateFamilyConsent(String parentUserId, String childUserId, boolean consented) {
        List<FamilyConsent> consents = familyConsentStore.getOrDefault(parentUserId, List.of());
        List<FamilyConsent> updated = new ArrayList<>();
        boolean found = false;
        for (FamilyConsent consent : consents) {
            if (consent.getChildUserId().equals(childUserId)) {
                updated.add(new FamilyConsent(parentUserId, childUserId, consented));
                found = true;
            } else {
                updated.add(consent);
            }
        }
        if (!found) {
            return false;
        }
        familyConsentStore.put(parentUserId, updated);
        return true;
    }

    @Override
    public boolean revokeFamilyConsent(String parentUserId, String childUserId) {
        List<FamilyConsent> consents = familyConsentStore.getOrDefault(parentUserId, List.of());
        List<FamilyConsent> filtered = consents.stream()
                .filter(consent -> !consent.getChildUserId().equals(childUserId))
                .toList();
        if (filtered.size() == consents.size()) {
            return false;
        }
        familyConsentStore.put(parentUserId, new ArrayList<>(filtered));
        return true;
    }

    @Override
    public List<FamilyConsent> getFamilyConsents(String parentUserId) {
        return familyConsentStore.getOrDefault(parentUserId, List.of());
    }

    @Override
    public FamilyDashboardSummary getFamilyDashboardSummary(String userId) {
        int alertCount = familyAlertPort.findByUserId(userId).size();
        int unusual = (int) familyAlertPort.findByUserId(userId).stream()
                .filter(it -> it.getMessage().contains("이상거래")).count();
        int deliveredReports = 1;
        return new FamilyDashboardSummary(userId, alertCount, unusual, deliveredReports);
    }

    @Override
    public TransferResult getTransfer(String transferId) {
        return transferStore.get(transferId);
    }

    @Override
    public List<TransferResult> getTransfers(String userId) {
        return transferHistoryByUser.getOrDefault(userId, List.of());
    }

    @Override
    public Map<String, Object> getAssetSummary(String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalAsset", snapshot.getTotalAsset());
        summary.put("monthlySpend", snapshot.getMonthlySpend());
        summary.put("volatilityPercent", snapshot.getVolatilityPercent());
        return summary;
    }

    @Override
    public List<Map<String, Object>> getAssetTrend(String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth now = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            YearMonth point = now.minusMonths(i);
            Map<String, Object> item = new HashMap<>();
            item.put("month", point.format(formatter));
            item.put("totalAsset", Math.round(snapshot.getTotalAsset() * (1 - (i * 0.01))));
            trend.add(item);
        }
        return trend;
    }

    @Override
    public boolean unlinkMyDataInstitution(String userId, String institutionCode) {
        linkedInstitutionStore.put(userId + "|" + institutionCode, false);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "UNLINK_INSTITUTION", institutionCode, "SUCCESS",
                "institutionCode=" + institutionCode, LocalDateTime.now()
        ));
        return true;
    }
}
