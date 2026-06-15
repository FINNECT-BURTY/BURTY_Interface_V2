/**
 *
 *
 * <pre>
 * <b>Description  : 관리 (BaseCodeSeeder)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.admin
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
package com.burty.application.service.admin;

import com.burty.core.code.CodeGroups;
import com.burty.core.constant.LogMessages;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.domain.admin.repository.BaseCodeRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BaseCodeSeeder {
  private static final Logger log = LoggerFactory.getLogger(BaseCodeSeeder.class);
  private static final String SEED_OPERATOR = "system-seed";

  private final BaseCodeRepository repository;
  private final BaseCodeService baseCodeService;

  public BaseCodeSeeder(BaseCodeRepository repository, BaseCodeService baseCodeService) {
    this.repository = repository;
    this.baseCodeService = baseCodeService;
  }

  @PostConstruct
  @Transactional
  public void seed() {
    long inserted = 0;
    inserted += seedAccountTypes();
    inserted += seedInstitutionTypes();
    inserted += seedExpenseCategories();
    inserted += seedIncomeCategories();
    inserted += seedScheduleTypes();
    inserted += seedActionTypes();
    inserted += seedRiskLevels();
    inserted += seedOccupationTypes();
    inserted += seedResidenceTypes();
    inserted += seedPolicyTypes();
    inserted += seedMerchantKeywords();
    inserted += seedNotificationTypes();
    inserted += seedAlertTypes();
    inserted += seedSectionTypes();
    inserted += seedConsentTypes();
    inserted += seedRelations();
    inserted += seedActorTypes();
    inserted += seedChannels();
    inserted += seedPurposes();
    inserted += seedPeriodTypes();
    inserted += seedEasyReadDict();
    log.info(LogMessages.Admin.BASE_CODE_SEEDER, inserted);
    if (inserted > 0) {
      baseCodeService.reload();
    }
  }

  private long seedAccountTypes() {
    return seedGroup(
        CodeGroups.ACCOUNT_TYPE,
        List.of(
            row("DEPOSIT", "입출금", "Deposit", 10, null, null, null),
            row("SAVINGS", "적금", "Savings", 20, null, null, null),
            row("CHECKING", "수시입출", "Checking", 30, null, null, null),
            row("STOCK", "주식", "Stock", 40, null, null, null),
            row("FUND", "펀드", "Fund", 50, null, null, null),
            row("PENSION", "연금", "Pension", 60, null, null, null),
            row("LOAN", "대출", "Loan", 70, null, null, null),
            row("CARD", "카드", "Card", 80, null, null, null)));
  }

  private long seedInstitutionTypes() {
    return seedGroup(
        CodeGroups.INSTITUTION_TYPE,
        List.of(
            row("BANK", "은행", "Bank", 10, null, null, null),
            row("CARD", "카드사", "Card Issuer", 20, null, null, null),
            row("SECURITIES", "증권사", "Securities", 30, null, null, null),
            row("PENSION", "연금기관", "Pension", 40, null, null, null),
            row("INSURANCE", "보험사", "Insurance", 50, null, null, null),
            row("P2P", "P2P", "P2P", 60, null, null, null),
            row("CAPITAL", "캐피탈", "Capital", 70, null, null, null)));
  }

  private long seedExpenseCategories() {
    return seedGroup(
        CodeGroups.EXPENSE_CATEGORY,
        List.of(
            row("RENT", "월세", "Rent", 10, null, "FIXED", null),
            row("CARD_BILL", "카드대금", "Card Bill", 20, null, "FIXED", null),
            row("LOAN", "대출상환", "Loan Repayment", 30, null, "FIXED", null),
            row("UTIL", "공과금", "Utilities", 40, null, "FIXED", null),
            row("COMM", "통신비", "Communication", 50, null, "FIXED", null),
            row("INSURANCE", "보험료", "Insurance", 60, null, "FIXED", null),
            row("FOOD", "식비", "Food", 70, null, "VARIABLE", null),
            row("TRANSPORT", "교통비", "Transport", 80, null, "VARIABLE", null),
            row("MEDICAL", "의료비", "Medical", 90, null, "VARIABLE", null),
            row("EDU", "교육비", "Education", 100, null, "VARIABLE", null),
            row("ENTERTAIN", "여가", "Entertainment", 110, null, "VARIABLE", null)));
  }

  private long seedIncomeCategories() {
    return seedGroup(
        CodeGroups.INCOME_CATEGORY,
        List.of(
            row("SALARY", "급여", "Salary", 10, null, "REGULAR", null),
            row("SIDE_JOB", "부수입", "Side Income", 20, null, "IRREGULAR", null),
            row("ALLOWANCE", "용돈", "Allowance", 30, null, "IRREGULAR", null),
            row("INVEST", "투자수익", "Investment", 40, null, "IRREGULAR", null),
            row("OTHER", "기타", "Other", 99, null, "IRREGULAR", null)));
  }

  private long seedScheduleTypes() {
    return seedGroup(
        CodeGroups.SCHEDULE_TYPE,
        List.of(
            row("RENT_DAY", "월세 결제일", "Rent Day", 10, "EXPENSE", "RENT", null),
            row("CARD_DAY", "카드 결제일", "Card Day", 20, "EXPENSE", "CARD_BILL", null),
            row("SALARY_DAY", "급여일", "Salary Day", 30, "INCOME", "SALARY", null),
            row("LOAN_DAY", "대출 상환일", "Loan Day", 40, "EXPENSE", "LOAN", null),
            row("UTIL_DAY", "공과금 결제일", "Utility Day", 50, "EXPENSE", "UTIL", null),
            row(
                "SUBSCRIPTION_DAY",
                "구독료 결제일",
                "Subscription Day",
                60,
                "EXPENSE",
                "ENTERTAIN",
                null)));
  }

  private long seedActionTypes() {
    return seedGroup(
        CodeGroups.ACTION_TYPE,
        List.of(
            row("FOOD_BUDGET_CUT", "식비 예산 조정", "Reduce Food Budget", 10, "85", "80000", null),
            row(
                "CARD_DUE_DATE_CHANGE",
                "카드 결제일 이동",
                "Move Card Due Date",
                20,
                "78",
                "120000",
                null),
            row(
                "DEBT_PRIORITY_CHANGE",
                "고금리 상환 우선",
                "Prioritize High-rate Debt",
                30,
                "65",
                "60000",
                null),
            row(
                "DEFER_NON_ESSENTIAL",
                "비필수 지출 보류",
                "Defer Non-essential",
                40,
                "72",
                "150000",
                null),
            row(
                "EMERGENCY_POLICY_CHECK",
                "긴급 정책 확인",
                "Check Emergency Policy",
                50,
                "90",
                "200000",
                null),
            row("SUBSCRIPTION_REVIEW", "구독 점검", "Review Subscriptions", 60, "60", "30000", null),
            row("EMERGENCY_FUND_SETUP", "비상금 적립", "Setup Emergency Fund", 70, "70", "50000", null),
            row("POLICY_APPLY", "정책 신청", "Apply Policy", 80, "75", "0", null),
            row("NO_ACTION", "현 상태 유지", "No Action", 99, "10", "0", null)));
  }

  private long seedRiskLevels() {
    return seedGroup(
        CodeGroups.RISK_LEVEL,
        List.of(
            row("GREEN", "안전", "Safe", 10, "#3DBE6F", "50000", "GREATER_EQUAL"),
            row("YELLOW", "주의", "Caution", 20, "#F5C147", "50000", "LESS_THAN"),
            row("RED", "위험", "Danger", 30, "#E64C4C", "0", "LESS_THAN")));
  }

  private long seedOccupationTypes() {
    return seedGroup(
        CodeGroups.OCCUPATION_TYPE,
        List.of(
            row("NEW_WORKER", "사회 초년생", "New Worker", 10, null, null, null),
            row("FREELANCER", "프리랜서", "Freelancer", 20, null, null, null),
            row("JOB_SEEKER", "구직자", "Job Seeker", 30, null, null, null),
            row("STUDENT", "학생", "Student", 40, null, null, null),
            row("PART_TIMER", "아르바이트", "Part Timer", 50, null, null, null),
            row("FULL_TIMER", "정규직", "Full Timer", 60, null, null, null)));
  }

  private long seedResidenceTypes() {
    return seedGroup(
        CodeGroups.RESIDENCE_TYPE,
        List.of(
            row("OWN", "자가", "Own", 10, null, null, null),
            row("JEONSE", "전세", "Jeonse", 20, null, null, null),
            row("MONTHLY_RENT", "월세", "Monthly Rent", 30, null, null, null),
            row("SHARE", "공유 주거", "Share House", 40, null, null, null),
            row("DORM", "기숙사", "Dormitory", 50, null, null, null),
            row("SEOUL", "서울 거주", "Seoul Resident", 60, null, null, null)));
  }

  private long seedPolicyTypes() {
    return seedGroup(
        CodeGroups.POLICY_TYPE,
        List.of(
            row("HOUSING", "주거 지원", "Housing", 10, null, null, null),
            row("EMPLOYMENT", "고용 지원", "Employment", 20, null, null, null),
            row("FINANCE", "금융 지원", "Finance", 30, null, null, null),
            row("EDUCATION", "교육 지원", "Education", 40, null, null, null),
            row("HEALTHCARE", "의료 지원", "Healthcare", 50, null, null, null)));
  }

  private long seedMerchantKeywords() {
    return seedGroup(
        CodeGroups.MERCHANT_KEYWORD,
        List.of(
            row("RENT", "월세", "Rent", 10, "RENT", "100", null),
            row("CARD_KW", "카드", "Card", 20, "CARD_BILL", "100", null),
            row("LOAN_KW", "대출", "Loan", 30, "LOAN", "100", null),
            row("KEPCO", "한전", "KEPCO", 40, "UTIL", "90", null),
            row("WATER", "수도", "Water", 50, "UTIL", "90", null),
            row("GAS", "가스", "Gas", 60, "UTIL", "90", null),
            row("TELECOM_KT", "KT", "KT", 70, "COMM", "80", null),
            row("TELECOM_SKT", "SKT", "SKT", 80, "COMM", "80", null),
            row("TELECOM_LG", "LGU", "LGU+", 90, "COMM", "80", null),
            row("STARBUCKS", "스타벅스", "Starbucks", 100, "FOOD", "70", null),
            row("MCDONALDS", "맥도날드", "McDonald's", 110, "FOOD", "70", null),
            row("CU", "CU", "CU", 120, "FOOD", "60", null),
            row("GS25", "GS25", "GS25", 130, "FOOD", "60", null),
            row("SUBWAY_TX", "지하철", "Subway", 140, "TRANSPORT", "80", null),
            row("BUS_TX", "버스", "Bus", 150, "TRANSPORT", "80", null),
            row("UBER", "택시", "Taxi", 160, "TRANSPORT", "80", null),
            row("PHARMACY", "약국", "Pharmacy", 170, "MEDICAL", "75", null),
            row("HOSPITAL", "병원", "Hospital", 180, "MEDICAL", "85", null),
            row("NETFLIX", "넷플릭스", "Netflix", 190, "ENTERTAIN", "85", null),
            row("MELON", "멜론", "Melon", 200, "ENTERTAIN", "75", null),
            row("INSURANCE_KW", "보험", "Insurance", 210, "INSURANCE", "85", null)));
  }

  private long seedNotificationTypes() {
    return seedGroup(
        CodeGroups.NOTIFICATION_TYPE,
        List.of(
            row("LARGE_TRANSFER", "대규모 이체", "Large Transfer", 10, null, null, null),
            row("UNUSUAL_LOGIN", "이상 로그인", "Unusual Login", 20, null, null, null),
            row("CONSENT_EXPIRING", "동의 만료 임박", "Consent Expiring", 30, null, null, null),
            row("BALANCE_LOW", "잔액 부족 경고", "Low Balance", 40, null, null, null),
            row("REPORT_DELIVERED", "월간 리포트 발송", "Monthly Report", 50, null, null, null)));
  }

  private long seedAlertTypes() {
    return seedGroup(
        CodeGroups.ALERT_TYPE,
        List.of(
            row("TRANSFER_OVER_AMOUNT", "고액 이체", "Transfer Over Amount", 10, null, null, null),
            row("LATE_NIGHT_TRANSACTION", "심야 거래", "Late Night Tx", 20, null, null, null),
            row("UNUSUAL_PATTERN", "이상 패턴", "Unusual Pattern", 30, null, null, null),
            row("LOGIN_NEW_DEVICE", "새 기기 로그인", "New Device", 40, null, null, null),
            row("CONSENT_EXPIRING", "동의 만료", "Consent Expiring", 50, null, null, null),
            row("BALANCE_BELOW_THRESHOLD", "잔액 임계 미만", "Balance Below", 60, null, null, null)));
  }

  private long seedSectionTypes() {
    return seedGroup(
        CodeGroups.SECTION_TYPE,
        List.of(
            row("SUMMARY", "요약", "Summary", 10, null, null, null),
            row("CASHFLOW", "현금흐름", "Cashflow", 20, null, null, null),
            row("RISK", "위험", "Risk", 30, null, null, null),
            row("ACTION", "행동 제안", "Action", 40, null, null, null),
            row("POLICY", "정책", "Policy", 50, null, null, null)));
  }

  private long seedConsentTypes() {
    return seedGroup(
        CodeGroups.CONSENT_TYPE,
        List.of(
            row("MYDATA", "마이데이터 수집", "MyData Collection", 10, null, null, null),
            row("FAMILY_NOTIFY", "가족 알림", "Family Notify", 20, null, null, null),
            row("MARKETING", "마케팅 수신", "Marketing", 30, null, null, null),
            row("ANALYTICS", "분석 활용", "Analytics", 40, null, null, null),
            row(
                "SECURITY_LOG",
                "접속 보안 로그",
                "Security Access Log",
                50,
                "IP/DEVICE/APPROX_REGION",
                "SECURITY",
                null),
            row(
                "LOCATION_POLICY_MATCH",
                "지역 기반 정책 추천",
                "Location Policy Match",
                60,
                "USER_RESIDENCE",
                "SERVICE",
                null)));
  }

  private long seedRelations() {
    return seedGroup(
        CodeGroups.RELATION,
        List.of(
            row("PARENT", "부모", "Parent", 10, null, null, null),
            row("CHILD", "자녀", "Child", 20, null, null, null),
            row("SPOUSE", "배우자", "Spouse", 30, null, null, null),
            row("SIBLING", "형제자매", "Sibling", 40, null, null, null),
            row("GUARDIAN", "보호자", "Guardian", 50, null, null, null),
            row("OTHER", "기타", "Other", 99, null, null, null)));
  }

  private long seedActorTypes() {
    return seedGroup(
        CodeGroups.ACTOR_TYPE,
        List.of(
            row("USER", "사용자", "User", 10, null, null, null),
            row("SYSTEM", "시스템", "System", 20, null, null, null),
            row("AI_AGENT", "AI 에이전트", "AI Agent", 30, null, null, null),
            row("BANK", "은행", "Bank", 40, null, null, null),
            row("SCHEDULER", "스케줄러", "Scheduler", 50, null, null, null)));
  }

  private long seedChannels() {
    return seedGroup(
        CodeGroups.CHANNEL,
        List.of(
            row("PUSH", "푸시", "Push", 10, null, null, null),
            row("SMS", "문자", "SMS", 20, null, null, null),
            row("EMAIL", "이메일", "Email", 30, null, null, null),
            row("ALL", "전체", "All", 99, null, null, null)));
  }

  private long seedPurposes() {
    return seedGroup(
        CodeGroups.PURPOSE,
        List.of(
            row("RENT", "월세 납부", "Rent", 10, null, null, null),
            row("UTIL", "공과금", "Utilities", 20, null, null, null),
            row("LIVING", "생활비", "Living", 30, null, null, null),
            row("EMERGENCY", "긴급", "Emergency", 40, null, null, null),
            row("SAVING", "저축/적립", "Saving", 50, null, null, null),
            row("OTHER", "기타", "Other", 99, null, null, null)));
  }

  private long seedPeriodTypes() {
    return seedGroup(
        CodeGroups.PERIOD_TYPE,
        List.of(
            row("DAILY", "일간", "Daily", 10, null, null, null),
            row("MONTHLY", "월간", "Monthly", 20, null, null, null),
            row("PER_TRANSACTION", "건당", "Per Transaction", 30, null, null, null)));
  }

  private long seedEasyReadDict() {
    return seedGroup(
        CodeGroups.EASY_READ_DICT,
        List.of(
            row("VOLATILITY", "변동성", "Volatility", 10, "출렁임", null, null),
            row("PORTFOLIO", "포트폴리오", "Portfolio", 20, "돈 묶음", null, null),
            row("SHARPE", "샤프 지수", "Sharpe Ratio", 30, "안정도", null, null),
            row("REBALANCE", "리밸런싱", "Rebalance", 40, "다시 나누기", null, null),
            row("CASHFLOW", "현금흐름", "Cashflow", 50, "들어오고 나가는 돈", null, null),
            row("LIQUIDITY", "유동성", "Liquidity", 60, "현금화 쉬움", null, null),
            row("DRAWDOWN", "낙폭", "Drawdown", 70, "최대 떨어진 폭", null, null)));
  }

  private long seedGroup(String group, List<SeedRow> rows) {
    long count = 0;
    LocalDateTime now = LocalDateTime.now();
    List<BaseCodeEntity> toSave = new ArrayList<>();
    for (SeedRow r : rows) {
      String codeId = group + "." + r.value();
      if (repository.existsById(codeId)) continue;
      BaseCodeEntity e = new BaseCodeEntity();
      e.setCodeId(codeId);
      e.setCodeGroup(group);
      e.setCodeValue(r.value());
      e.setCodeNameKo(r.nameKo());
      e.setCodeNameEn(r.nameEn());
      e.setSortOrder(r.sort());
      e.setUseYn("Y");
      e.setAttr1(r.attr1());
      e.setAttr2(r.attr2());
      e.setAttr3(r.attr3());
      e.setCreatedAt(now);
      e.setUpdatedAt(now);
      e.setCreatedBy(SEED_OPERATOR);
      e.setUpdatedBy(SEED_OPERATOR);
      toSave.add(e);
      count++;
    }
    if (!toSave.isEmpty()) {
      repository.saveAll(toSave);
    }
    return count;
  }

  private static SeedRow row(
      String value, String ko, String en, int sort, String attr1, String attr2, String attr3) {
    return new SeedRow(value, ko, en, sort, attr1, attr2, attr3);
  }

  private record SeedRow(
      String value,
      String nameKo,
      String nameEn,
      int sort,
      String attr1,
      String attr2,
      String attr3) {}
}
