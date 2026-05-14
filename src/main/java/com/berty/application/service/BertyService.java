package com.berty.application.service;

import com.berty.core.exception.BusinessException;
import com.berty.core.error.enums.ErrorCode;
import com.berty.application.port.in.ActionRecommendationUseCase;
import com.berty.application.port.in.BertyUseCase;
import com.berty.application.port.in.CashflowForecastUseCase;
import com.berty.application.port.in.PersonaInferenceUseCase;
import com.berty.application.port.in.RiskAssessmentUseCase;
import com.berty.application.port.out.*;
import com.berty.domain.entity.ActionExecutionEntity;
import com.berty.domain.entity.ActionFeedbackEntity;
import com.berty.domain.entity.ActionFeedbackScoreEntity;
import com.berty.domain.entity.FamilyConsentEntity;
import com.berty.domain.entity.PersonaProfileEntity;
import com.berty.domain.entity.RegisteredAccountEntity;
import com.berty.domain.entity.TransferRecordEntity;
import com.berty.domain.entity.UserSettingEntity;
import com.berty.domain.model.*;
import com.berty.domain.repository.ActionExecutionRepository;
import com.berty.domain.repository.ActionFeedbackRepository;
import com.berty.domain.repository.ActionFeedbackScoreRepository;
import com.berty.domain.repository.FamilyConsentRepository;
import com.berty.domain.repository.MyDataLinkStatusRepository;
import com.berty.domain.repository.RegisteredAccountRepository;
import com.berty.domain.repository.TransferRecordRepository;
import com.berty.domain.repository.UserSettingRepository;
import com.berty.util.AccountNumberHasher;
import com.berty.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BertyService implements BertyUseCase {
    private static final long FAMILY_ALERT_THRESHOLD = 1_000_000L;
    private static final long LARGE_TRANSFER_THRESHOLD = 3_000_000L;
    private static final Logger log = LoggerFactory.getLogger(BertyService.class);

    private final EasyReadPort easyReadPort;
    private final MyDataPort myDataPort;
    private final BiometricAuthPort biometricAuthPort;
    private final OpenBankingPort openBankingPort;
    private final FamilyAlertPort familyAlertPort;
    private final AuditLogPort auditLogPort;
    private final PolicyMatchingService policyMatchingService;
    private final CashflowForecastUseCase cashflowForecastUseCase;
    private final RiskAssessmentUseCase riskAssessmentUseCase;
    private final ActionRecommendationUseCase actionRecommendationUseCase;
    private final ActionExecutionRepository actionExecutionRepository;
    private final ActionFeedbackRepository actionFeedbackRepository;
    private final ActionFeedbackScoreRepository actionFeedbackScoreRepository;
    private final FamilyConsentRepository familyConsentRepository;
    private final UserSettingRepository userSettingRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final RegisteredAccountRepository registeredAccountRepository;
    private final PersonaInferenceUseCase personaInferenceUseCase;
    private final AccountNumberHasher accountNumberHasher;
    private final EncryptionUtil encryptionUtil;
    private final MyDataLinkStatusRepository myDataLinkStatusRepository;

    private static final String LIMIT_KEY = "TRANSFER_LIMIT";
    private static final String SAFETY_BALANCE_KEY = "SAFETY_BALANCE";
    private static final String OPENING_BALANCE_OVERRIDE_KEY = "OPENING_BALANCE_OVERRIDE";
    private static final String MONTHLY_VARIABLE_BUDGET_KEY = "MONTHLY_VARIABLE_BUDGET";

    public BertyService(EasyReadPort easyReadPort,
                        MyDataPort myDataPort,
                        BiometricAuthPort biometricAuthPort,
                        OpenBankingPort openBankingPort,
                        FamilyAlertPort familyAlertPort,
                        AuditLogPort auditLogPort,
                        PolicyMatchingService policyMatchingService,
                        CashflowForecastUseCase cashflowForecastUseCase,
                        RiskAssessmentUseCase riskAssessmentUseCase,
                        ActionRecommendationUseCase actionRecommendationUseCase,
                        ActionExecutionRepository actionExecutionRepository,
                        ActionFeedbackRepository actionFeedbackRepository,
                        ActionFeedbackScoreRepository actionFeedbackScoreRepository,
                        FamilyConsentRepository familyConsentRepository,
                        UserSettingRepository userSettingRepository,
                        TransferRecordRepository transferRecordRepository,
                        RegisteredAccountRepository registeredAccountRepository,
                        PersonaInferenceUseCase personaInferenceUseCase,
                        AccountNumberHasher accountNumberHasher,
                        EncryptionUtil encryptionUtil,
                        MyDataLinkStatusRepository myDataLinkStatusRepository) {
        this.easyReadPort = easyReadPort;
        this.myDataPort = myDataPort;
        this.biometricAuthPort = biometricAuthPort;
        this.openBankingPort = openBankingPort;
        this.familyAlertPort = familyAlertPort;
        this.auditLogPort = auditLogPort;
        this.policyMatchingService = policyMatchingService;
        this.cashflowForecastUseCase = cashflowForecastUseCase;
        this.riskAssessmentUseCase = riskAssessmentUseCase;
        this.actionRecommendationUseCase = actionRecommendationUseCase;
        this.actionExecutionRepository = actionExecutionRepository;
        this.actionFeedbackRepository = actionFeedbackRepository;
        this.actionFeedbackScoreRepository = actionFeedbackScoreRepository;
        this.familyConsentRepository = familyConsentRepository;
        this.userSettingRepository = userSettingRepository;
        this.transferRecordRepository = transferRecordRepository;
        this.registeredAccountRepository = registeredAccountRepository;
        this.personaInferenceUseCase = personaInferenceUseCase;
        this.accountNumberHasher = accountNumberHasher;
        this.encryptionUtil = encryptionUtil;
        this.myDataLinkStatusRepository = myDataLinkStatusRepository;
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
        CashflowForecast forecast = getCashflowForecast(userId);
        RiskAssessment risk = getCashflowRisk(userId);
        ActionRecommendation action = getTopActionRecommendation(userId);
        String summary = easyReadPort.toEasyRead(
                "30일 현금흐름 점검 결과 최소 잔액은 %,d원입니다. 위험 단계는 %s 입니다. "
                        .formatted(forecast.getMinimumBalance(), risk.getLevel())
                        + "권장 행동은 " + action.getTitle() + " 입니다."
        );
        return new MonthlyReport(
                userId,
                YearMonth.now().toString(),
                summary,
                risk.getLevel(),
                action.getTitle(),
                List.of(
                        "예상 위험일: " + (risk.getRiskDate() == null ? "없음" : risk.getRiskDate()),
                        "위험 근거: " + risk.getReason(),
                        "예상 개선효과: " + action.getEstimatedImprovement() + "원"
                )
        );
    }

    @Override
    public TransferResult transfer(String userId, String fromAccount, String toAccount, long amount, String description, String assertionToken) {
        if (!biometricAuthPort.verifyAssertion(userId, assertionToken)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "FIDO2/WebAuthn 인증 검증에 실패했습니다.");
        }

        boolean notify = amount >= FAMILY_ALERT_THRESHOLD;
        boolean unusualNightTransfer = LocalTime.now().isAfter(LocalTime.of(23, 0)) || LocalTime.now().isBefore(LocalTime.of(6, 0));
        boolean unregisteredAccountTransfer = !registeredAccountRepository
                .existsByUserIdAndAccountNoHash(userId, accountNumberHasher.hash(toAccount));
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
        TransferRecordEntity record = new TransferRecordEntity();
        record.setTransferId(result.getTransferId());
        record.setUserId(userId);
        record.setFromAccount(accountNumberHasher.mask(fromAccount));
        record.setToAccount(accountNumberHasher.mask(toAccount));
        record.setAmount(amount);
        record.setStatus(result.getStatus());
        record.setFamilyNotified(notify);
        record.setDescription(description);
        transferRecordRepository.save(record);
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
        UserSettingEntity setting = userSettingRepository.findByUserIdAndSettingKey(userId, LIMIT_KEY)
                .orElseGet(UserSettingEntity::new);
        setting.setUserId(userId);
        setting.setSettingKey(LIMIT_KEY);
        setting.setSettingValueLong(newLimit);
        userSettingRepository.save(setting);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "UPDATE_LIMIT", "LIMIT", "SUCCESS",
                "newLimit=" + newLimit, LocalDateTime.now()
        ));
    }

    @Override
    public long getLimit(String userId) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, LIMIT_KEY)
                .map(UserSettingEntity::getSettingValueLong)
                .orElse(0L);
    }

    @Override
    public void registerFamilyConsent(String parentUserId, String childUserId) {
        FamilyConsentEntity entity = familyConsentRepository
                .findByParentUserIdAndChildUserId(parentUserId, childUserId)
                .orElseGet(FamilyConsentEntity::new);
        entity.setParentUserId(parentUserId);
        entity.setChildUserId(childUserId);
        entity.setConsented(true);
        familyConsentRepository.save(entity);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), parentUserId, "REGISTER_FAMILY_CONSENT", childUserId, "SUCCESS",
                "consent=true", LocalDateTime.now()
        ));
    }

    @Override
    public boolean updateFamilyConsent(String parentUserId, String childUserId, boolean consented) {
        return familyConsentRepository.findByParentUserIdAndChildUserId(parentUserId, childUserId)
                .map(entity -> {
                    entity.setConsented(consented);
                    familyConsentRepository.save(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public boolean revokeFamilyConsent(String parentUserId, String childUserId) {
        return familyConsentRepository.findByParentUserIdAndChildUserId(parentUserId, childUserId)
                .map(entity -> {
                    familyConsentRepository.delete(entity);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<FamilyConsent> getFamilyConsents(String parentUserId) {
        return familyConsentRepository.findByParentUserId(parentUserId).stream()
                .map(e -> new FamilyConsent(e.getParentUserId(), e.getChildUserId(), Boolean.TRUE.equals(e.getConsented())))
                .toList();
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
        return transferRecordRepository.findById(transferId)
                .map(r -> new TransferResult(r.getTransferId(), r.getStatus(), Boolean.TRUE.equals(r.getFamilyNotified())))
                .orElse(null);
    }

    @Override
    public List<TransferResult> getTransfers(String userId) {
        return transferRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> new TransferResult(r.getTransferId(), r.getStatus(), Boolean.TRUE.equals(r.getFamilyNotified())))
                .toList();
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
        myDataLinkStatusRepository.findByUserIdAndInstitutionCode(userId, institutionCode)
                .ifPresent(entity -> {
                    entity.setStatus("UNLINKED");
                    entity.setUnlinkedAt(LocalDateTime.now());
                    myDataLinkStatusRepository.save(entity);
                });
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "UNLINK_INSTITUTION", institutionCode, "SUCCESS",
                "institutionCode=" + institutionCode, LocalDateTime.now()
        ));
        return true;
    }

    @Override
    public void updateCashflowCriteria(String userId, Long safetyBalance, Long openingBalanceOverride, Long monthlyVariableBudget) {
        if (safetyBalance != null) upsertLongSetting(userId, SAFETY_BALANCE_KEY, nonNegative(safetyBalance, "안전잔액"));
        if (openingBalanceOverride != null) upsertLongSetting(userId, OPENING_BALANCE_OVERRIDE_KEY, nonNegative(openingBalanceOverride, "현재잔액"));
        if (monthlyVariableBudget != null) upsertLongSetting(userId, MONTHLY_VARIABLE_BUDGET_KEY, nonNegative(monthlyVariableBudget, "월 변동지출 예산"));
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "UPDATE_CASHFLOW_CRITERIA", "USER_SETTING", "SUCCESS",
                "safetyBalance=" + safetyBalance + ", openingBalanceOverride=" + openingBalanceOverride
                        + ", monthlyVariableBudget=" + monthlyVariableBudget,
                LocalDateTime.now()
        ));
    }

    @Override
    public Map<String, Object> getCashflowCriteria(String userId) {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("userId", userId);
        criteria.put("safetyBalance", getLongSetting(userId, SAFETY_BALANCE_KEY));
        criteria.put("openingBalanceOverride", getLongSetting(userId, OPENING_BALANCE_OVERRIDE_KEY));
        criteria.put("monthlyVariableBudget", getLongSetting(userId, MONTHLY_VARIABLE_BUDGET_KEY));
        criteria.put("sourcePriority", List.of("USER_CUSTOM_CRITERIA", "MYDATA_SCHEDULES", "MYDATA_ASSET_FALLBACK"));
        return criteria;
    }

    @Override
    public CashflowForecast getCashflowForecast(String userId) {
        return cashflowForecastUseCase.forecast(userId);
    }

    @Override
    public RiskAssessment getCashflowRisk(String userId) {
        return riskAssessmentUseCase.assess(userId);
    }

    @Override
    public ActionRecommendation getTopActionRecommendation(String userId) {
        return actionRecommendationUseCase.topRecommendation(userId);
    }

    @Override
    public List<PolicyMatch> getPolicyMatches(String userId) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        int age = inferAgeFromUserId(userId);
        long monthlyIncome = estimateMonthlyIncome(snapshot);
        String lifeStage = inferLifeStage(snapshot);
        List<PolicyMatch> matches = policyMatchingService.matchPolicies(userId, age, monthlyIncome, lifeStage);
        log.info("KPI policy userId={} matchCount={}", userId, matches.size());
        return matches;
    }

    @Override
    public void applyPolicy(String userId, String policyCode) {
        policyMatchingService.markApplied(userId, policyCode);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "POLICY_APPLY", policyCode, "SUCCESS",
                "policyCode=" + policyCode, LocalDateTime.now()
        ));
    }

    @Override
    public List<RecurringExpense> detectRecurringExpenses(String userId, String fintechUseNum) {
        Map<String, Object> txResponse = openBankingPort.getTransactions(userId, fintechUseNum);
        Object txObj = txResponse.get("transactions");
        Map<String, RecurringExpenseAccumulator> accumulators = new HashMap<>();
        if (txObj instanceof List<?> txList) {
            for (Object item : txList) {
                if (!(item instanceof Map<?, ?> txMap)) {
                    continue;
                }
                Object typeObj = txMap.get("type");
                String type = typeObj == null ? "WITHDRAWAL" : String.valueOf(typeObj);
                if (!"WITHDRAWAL".equalsIgnoreCase(type)) {
                    continue;
                }
                long amount = extractLong(txMap.get("amount"), 0L);
                Object memoObj = txMap.get("memo");
                String memo = memoObj == null ? "기타" : String.valueOf(memoObj);
                if (amount <= 0) {
                    continue;
                }
                RecurringExpenseAccumulator current = accumulators.getOrDefault(memo, new RecurringExpenseAccumulator(0L, 0));
                accumulators.put(memo, new RecurringExpenseAccumulator(current.averageAmount + amount, current.count + 1));
            }
        }

        List<RecurringExpense> detected = new ArrayList<>();
        for (Map.Entry<String, RecurringExpenseAccumulator> entry : accumulators.entrySet()) {
            if (entry.getValue().count >= 1) {
                long avg = Math.max(10_000L, entry.getValue().averageAmount / entry.getValue().count);
                int day = inferDayByMemo(entry.getKey());
                detected.add(new RecurringExpense(entry.getKey(), avg, day));
            }
        }
        if (detected.isEmpty()) {
            AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
            long spend = Math.round(snapshot.getMonthlySpend());
            detected = List.of(
                    new RecurringExpense("월세", (long) (spend * 0.32), 25),
                    new RecurringExpense("카드값", (long) (spend * 0.25), 15),
                    new RecurringExpense("공과금", (long) (spend * 0.10), 21)
            );
        }
        return detected.stream()
                .sorted(java.util.Comparator.comparingLong(RecurringExpense::getAmount).reversed())
                .limit(5)
                .toList();
    }

    @Override
    public ActionExecutionResult executeRecommendedAction(String userId, String actionType) {
        String message = switch (actionType) {
            case "FOOD_BUDGET_CUT" -> "식비 예산 자동 조정이 적용되었습니다.";
            case "CARD_DUE_DATE_CHANGE" -> "카드 결제일 변경 신청 화면으로 연결했습니다.";
            case "DEBT_PRIORITY_CHANGE" -> "고금리 부채부터 확인할 수 있도록 참고 순서를 보여줍니다. 실제 상환 조건 변경은 금융기관 확인이 필요합니다.";
            case "EMERGENCY_POLICY_CHECK" -> "긴급 생활지원 정책 목록을 우선 노출했습니다.";
            default -> "행동 실행 요청이 접수되었습니다.";
        };
        ActionExecutionEntity executionEntity = new ActionExecutionEntity();
        executionEntity.setUserId(userId);
        executionEntity.setActionType(actionType);
        executionEntity.setExecuted(true);
        executionEntity.setMessage(message);
        executionEntity.setExecutedAt(LocalDateTime.now());
        actionExecutionRepository.save(executionEntity);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "EXECUTE_ACTION", actionType, "SUCCESS",
                buildExecutionAuditDetail(userId, actionType), LocalDateTime.now()
        ));
        return new ActionExecutionResult(userId, actionType, true, message);
    }

    private String buildExecutionAuditDetail(String userId, String actionType) {
        StringBuilder sb = new StringBuilder("actionType=").append(actionType);
        try {
            PersonaProfileEntity persona = personaInferenceUseCase.getOrInfer(userId);
            if (persona != null) {
                if (persona.getOccupationCode() != null) sb.append(",occupation=").append(persona.getOccupationCode());
                if (persona.getResidenceCode() != null) sb.append(",residence=").append(persona.getResidenceCode());
            }
            RiskAssessment risk = riskAssessmentUseCase.assess(userId);
            if (risk != null) {
                sb.append(",riskLevel=").append(risk.getLevel());
                sb.append(",projectedBalance=").append(risk.getProjectedBalance());
                if (risk.getRiskDate() != null) sb.append(",riskDate=").append(risk.getRiskDate());
            }
        } catch (Exception e) {
            log.debug("audit context enrichment skipped userId={} err={}", userId, e.getMessage());
        }
        return sb.toString();
    }

    @Override
    public void submitRecommendationFeedback(String userId, String actionType, String feedback) {
        boolean accept = "accept".equalsIgnoreCase(feedback);
        boolean reject = "reject".equalsIgnoreCase(feedback);

        ActionFeedbackScoreEntity score = actionFeedbackScoreRepository
                .findByUserIdAndActionTypeCode(userId, actionType)
                .orElseGet(() -> {
                    ActionFeedbackScoreEntity created = new ActionFeedbackScoreEntity();
                    created.setUserId(userId);
                    created.setActionTypeCode(actionType);
                    return created;
                });
        if (accept) score.setAcceptCount(score.getAcceptCount() + 1);
        if (reject) score.setRejectCount(score.getRejectCount() + 1);
        score.setScore(score.getAcceptCount() - score.getRejectCount());
        actionFeedbackScoreRepository.save(score);

        ActionFeedbackEntity feedbackEntity = new ActionFeedbackEntity();
        feedbackEntity.setUserId(userId);
        feedbackEntity.setActionType(actionType);
        feedbackEntity.setFeedback(feedback);
        feedbackEntity.setCreatedAt(LocalDateTime.now());
        actionFeedbackRepository.save(feedbackEntity);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), userId, "ACTION_FEEDBACK", actionType, "SUCCESS",
                "feedback=" + feedback, LocalDateTime.now()
        ));
        log.info("KPI action_feedback userId={} actionType={} feedback={} score={}",
                userId, actionType, feedback, score.getScore());
    }

    @Override
    public ActionFeedbackSummary getActionFeedbackSummary(String userId) {
        List<String> recent = actionExecutionRepository.findTop5ByUserIdOrderByExecutedAtDesc(userId).stream()
                .map(ActionExecutionEntity::getActionType)
                .toList();
        long executedCount = actionExecutionRepository.countByUserId(userId);
        long accepted = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "accept");
        long rejected = actionFeedbackRepository.countByUserIdAndFeedbackIgnoreCase(userId, "reject");
        return new ActionFeedbackSummary(
                userId,
                (int) executedCount,
                (int) accepted,
                (int) rejected,
                recent
        );
    }

    private long estimateMonthlyIncome(AssetSnapshot snapshot) {
        long spend = Math.round(snapshot.getMonthlySpend());
        return Math.max(1_000_000L, Math.round(spend * 1.1));
    }

    private void upsertLongSetting(String userId, String key, long value) {
        UserSettingEntity setting = userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .orElseGet(UserSettingEntity::new);
        setting.setUserId(userId);
        setting.setSettingKey(key);
        setting.setSettingValueLong(value);
        userSettingRepository.save(setting);
    }

    private Long getLongSetting(String userId, String key) {
        return userSettingRepository.findByUserIdAndSettingKey(userId, key)
                .map(UserSettingEntity::getSettingValueLong)
                .orElse(null);
    }

    private long nonNegative(long value, String label) {
        if (value < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, label + "은 0 이상이어야 합니다.");
        }
        return value;
    }

    private String inferLifeStage(AssetSnapshot snapshot) {
        double volatility = snapshot.getVolatilityPercent();
        if (volatility > 20) {
            return "freelancer";
        }
        if (snapshot.getMonthlySpend() < 2_000_000) {
            return "job_seeker";
        }
        return "worker";
    }

    private int inferAgeFromUserId(String userId) {
        int base = Math.abs(userId.hashCode() % 10);
        return 24 + base;
    }

    private int inferDayByMemo(String memo) {
        String lower = memo.toLowerCase();
        if (lower.contains("rent") || lower.contains("월세")) return 25;
        if (lower.contains("card") || lower.contains("카드")) return 15;
        if (lower.contains("loan") || lower.contains("대출")) return 12;
        return 20;
    }

    private long extractLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static class RecurringExpenseAccumulator {
        private final long averageAmount;
        private final int count;

        private RecurringExpenseAccumulator(long averageAmount, int count) {
            this.averageAmount = averageAmount;
            this.count = count;
        }
    }
}
