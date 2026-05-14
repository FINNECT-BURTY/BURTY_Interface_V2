package com.berty.prompt;

import com.berty.domain.entity.PersonaProfileEntity;
import com.berty.domain.model.AssetSnapshot;
import com.berty.domain.model.RiskAssessment;

/**
 * BERTY LLM 프롬프트 카탈로그.
 *
 * 시스템 역할 프롬프트(상수)와 컨텍스트 주입형 사용자 프롬프트 빌더(메서드)를 한 곳에 모은다.
 * 새 use-case가 추가되면 여기에 상수 + 빌더 한 쌍을 추가한다.
 */
public final class BertyPrompts {

    private BertyPrompts() {}

    // -----------------------------------------------------------------------
    // SYSTEM ROLE PROMPTS
    // -----------------------------------------------------------------------

    /** AI 상담 — 페르소나/위험단계에 맞춘 톤으로 30일 안에 실행할 단일 행동 안내 */
    public static final String SYSTEM_FINANCIAL_ADVISOR = """
            당신은 서울 청년 1인 가구를 돕는 생활금융 에이전트다.
            답변 규칙:
            1) 최대 3문장
            2) 전문용어 최소화, 구체 금액·날짜 포함
            3) 마지막 문장은 30일 안에 실행할 단일 행동 1개로 마무리
            4) 사용자의 직업/거주/위험단계에 맞춰 톤을 조정
            """;

    /** 월간 리포트 — 데이터 기반 요약, 다음달 행동 1~2개 권장 */
    public static final String SYSTEM_MONTHLY_REPORT_WRITER = """
            당신은 청년 1인 가구를 위한 월간 가계 리포트 작성자다.
            작성 규칙:
            1) 최대 5문장
            2) 숫자는 천 단위 콤마 + 단위(원)
            3) 위험단계가 RED/YELLOW면 위험 사유와 권장 행동을 반드시 포함
            4) 마지막 문장은 다음 달 첫 주 안에 실행할 행동 1~2개
            """;

    /** 행동 추천 설명 — 왜 이 행동인지를 1~2문장으로 풀어 설명 */
    public static final String SYSTEM_RECOMMENDATION_EXPLAINER = """
            당신은 행동 추천의 이유를 짧게 설명하는 코치다.
            설명 규칙:
            1) 1~2문장
            2) 사용자의 페르소나(직업/거주)와 잔액 위험을 근거로 사용
            3) 명령조 금지, "~하면 좋아요" 톤
            """;

    // -----------------------------------------------------------------------
    // USER PROMPT BUILDERS
    // -----------------------------------------------------------------------

    /**
     * AI 상담용 user prompt — 페르소나/자산/위험진단 컨텍스트 주입.
     */
    public static String advisoryUserPrompt(String question,
                                            PersonaProfileEntity persona,
                                            AssetSnapshot snapshot,
                                            RiskAssessment risk) {
        return """
                사용자 질문: %s
                페르소나:
                - 직업: %s, 거주: %s, 세대: %s, 추정 월소득: %,d원, 소득 변동성: %.1f%%
                현재 자산:
                - 총자산: %,.0f원, 월지출: %,.0f원, 변동성: %.1f%%
                30일 위험 진단:
                - 위험단계: %s, 최소 잔액: %,d원, 위험일: %s
                - 사유: %s
                """.formatted(
                        nullSafe(question),
                        occupation(persona), residence(persona), household(persona),
                        monthlyIncome(persona), variability(persona),
                        snapshot.getTotalAsset(), snapshot.getMonthlySpend(), snapshot.getVolatilityPercent(),
                        riskLevel(risk), projectedBalance(risk), riskDate(risk), riskReason(risk)
                );
    }

    /**
     * 월간 리포트용 user prompt — 직전 달 핵심 수치 + 위험/행동 요약.
     */
    public static String monthlyReportUserPrompt(String period,
                                                 PersonaProfileEntity persona,
                                                 AssetSnapshot snapshot,
                                                 RiskAssessment risk,
                                                 String topActionTitle,
                                                 long estimatedImprovement) {
        return """
                대상 기간: %s
                페르소나: 직업 %s / 거주 %s / 추정 월소득 %,d원
                자산 스냅샷: 총자산 %,.0f원 / 월지출 %,.0f원 / 변동성 %.1f%%
                30일 위험: %s / 최소 잔액 %,d원 / 위험일 %s / 사유 %s
                추천 행동: %s (예상 개선효과 %,d원)
                """.formatted(
                        nullSafe(period),
                        occupation(persona), residence(persona), monthlyIncome(persona),
                        snapshot.getTotalAsset(), snapshot.getMonthlySpend(), snapshot.getVolatilityPercent(),
                        riskLevel(risk), projectedBalance(risk), riskDate(risk), riskReason(risk),
                        nullSafe(topActionTitle), estimatedImprovement
                );
    }

    /**
     * 행동 추천 설명용 user prompt.
     */
    public static String recommendationExplainerUserPrompt(String actionTypeCode,
                                                           String actionTitle,
                                                           PersonaProfileEntity persona,
                                                           RiskAssessment risk) {
        return """
                추천 행동 코드: %s
                추천 행동 제목: %s
                페르소나: 직업 %s / 거주 %s
                위험 단계: %s (최소 잔액 %,d원)
                위 정보를 근거로 왜 이 행동이 지금 적합한지 1~2문장으로 설명해라.
                """.formatted(
                        nullSafe(actionTypeCode), nullSafe(actionTitle),
                        occupation(persona), residence(persona),
                        riskLevel(risk), projectedBalance(risk)
                );
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static String nullSafe(String s) { return s == null ? "-" : s; }

    private static String occupation(PersonaProfileEntity p) {
        return p == null ? "-" : nullSafe(p.getOccupationCode());
    }

    private static String residence(PersonaProfileEntity p) {
        return p == null ? "-" : nullSafe(p.getResidenceCode());
    }

    private static String household(PersonaProfileEntity p) {
        return p == null ? "-" : nullSafe(p.getHouseholdType());
    }

    private static long monthlyIncome(PersonaProfileEntity p) {
        if (p == null || p.getMonthlyIncomeAvg() == null) return 0L;
        return p.getMonthlyIncomeAvg();
    }

    private static double variability(PersonaProfileEntity p) {
        if (p == null || p.getIncomeVariabilityPct() == null) return 0.0;
        return p.getIncomeVariabilityPct();
    }

    private static String riskLevel(RiskAssessment r) {
        return r == null ? "-" : nullSafe(r.getLevel());
    }

    private static long projectedBalance(RiskAssessment r) {
        return r == null ? 0L : r.getProjectedBalance();
    }

    private static String riskDate(RiskAssessment r) {
        if (r == null || r.getRiskDate() == null) return "없음";
        return r.getRiskDate().toString();
    }

    private static String riskReason(RiskAssessment r) {
        return r == null ? "-" : nullSafe(r.getReason());
    }
}