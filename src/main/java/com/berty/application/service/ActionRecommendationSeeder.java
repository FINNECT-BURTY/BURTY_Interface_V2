package com.berty.application.service;

import com.berty.domain.entity.ActionRecommendationEntity;
import com.berty.domain.repository.ActionRecommendationRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ActionRecommendationSeeder {
    private static final Logger log = LoggerFactory.getLogger(ActionRecommendationSeeder.class);

    private final ActionRecommendationRepository repository;

    public ActionRecommendationSeeder(ActionRecommendationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        long inserted = 0;
        inserted += upsert("REC_FOOD_CUT_DEFAULT", "FOOD_BUDGET_CUT",
                "이번 주 식비 20% 조정",
                "3주간 식비를 20% 줄이면 월말 잔액 방어에 가장 빠르게 효과가 납니다.",
                85.0, 80_000L, null, null, null);
        inserted += upsert("REC_CARD_DUE_DEFAULT", "CARD_DUE_DATE_CHANGE",
                "카드 결제일 다음 달 초로 이동",
                "결제일을 급여일 이후로 조정하면 현금흐름 충돌을 줄일 수 있습니다.",
                78.0, 120_000L, null, null, null);
        inserted += upsert("REC_DEBT_PRIORITY_DEFAULT", "DEBT_PRIORITY_CHANGE",
                "고금리 상환 우선순위 조정",
                "고금리 대출 상환 순서를 조정해 다음 달 이자부담을 줄이세요.",
                65.0, 60_000L, null, null, null);
        inserted += upsert("REC_DEFER_DEFAULT", "DEFER_NON_ESSENTIAL",
                "비필수 지출 일시 보류",
                "대형 소비를 2주만 늦추면 위험구간을 회피할 확률이 높아집니다.",
                72.0, 150_000L, null, null, null);
        inserted += upsert("REC_EMERGENCY_POLICY_NEGATIVE", "EMERGENCY_POLICY_CHECK",
                "긴급 생활지원 정책 우선 확인",
                "마이너스 잔액이 예상되어 정책성 지원금 확인이 최우선입니다.",
                95.0, 200_000L, null, null, 0L);
        inserted += upsert("REC_SUBSCRIPTION_DEFAULT", "SUBSCRIPTION_REVIEW",
                "구독 점검 후 정리",
                "사용 빈도 낮은 구독을 정리하면 매월 고정 지출이 줄어듭니다.",
                60.0, 30_000L, null, null, null);
        inserted += upsert("REC_EMERGENCY_FUND_FREELANCER", "EMERGENCY_FUND_SETUP",
                "프리랜서 비상금 자동 적립",
                "수입 변동이 큰 프리랜서는 월 5만원 자동이체로 비상금을 모으세요.",
                80.0, 50_000L, "FREELANCER", null, null);
        inserted += upsert("REC_EMERGENCY_FUND_DEFAULT", "EMERGENCY_FUND_SETUP",
                "월 5만원 비상금 자동 적립",
                "월 5만원 자동이체로 비상금을 모으세요.",
                65.0, 50_000L, null, null, null);
        inserted += upsert("REC_POLICY_APPLY_JOBSEEKER", "POLICY_APPLY",
                "구직자 지원 정책 즉시 신청",
                "구직 중이라면 청년 긴급생활비 등 즉시 신청 가능한 정책을 확인하세요.",
                85.0, 0L, "JOB_SEEKER", null, null);
        log.info("ActionRecommendation seeder inserted={} rows", inserted);
    }

    private long upsert(String recId, String actionType, String title, String desc, double base, long improvement, String occupationCode, Long minMin, Long maxMin) {
        if (repository.existsById(recId)) return 0;
        ActionRecommendationEntity e = new ActionRecommendationEntity();
        e.setRecId(recId);
        e.setActionTypeCode(actionType);
        e.setTitleTemplate(title);
        e.setDescriptionTemplate(desc);
        e.setBaseScore(base);
        e.setEstimatedImprovement(improvement);
        e.setOccupationCode(occupationCode);
        e.setMinMinBalance(minMin);
        e.setMaxMinBalance(maxMin);
        e.setActive(true);
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        repository.saveAll(List.of(e));
        return 1;
    }
}
