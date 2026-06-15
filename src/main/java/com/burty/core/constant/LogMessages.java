package com.burty.core.constant;

/** 애플리케이션 로그 메시지 상수 (한글). */
public final class LogMessages {

  private LogMessages() {}

  public static final class Admin {
    public static final String BASE_CODE_CACHE_LOADED = "기준코드 캐시 적재 완료 groups={} totalRows={}";
    public static final String BASE_CODE_SEEDER = "기준코드 시드 완료 inserted={} rows";
    public static final String CATEGORY_RULE_SEEDER = "카테고리 룰 시드 완료 inserted={} rows";
    public static final String ACTION_RECOMMENDATION_SEEDER = "행동 추천 시드 완료 inserted={} rows";
    public static final String POLICY_SEEDER = "정책 시드 완료 inserted={} rows";

    private Admin() {}
  }

  public static final class Action {
    public static final String RECOMMENDATION_KPI =
        "KPI 행동 추천 userId={} action={} score={} occupation={} candidates={} epsilon={}";
    public static final String FEEDBACK_KPI =
        "KPI 행동 피드백 userId={} actionType={} feedback={} score={}";

    private Action() {}
  }

  public static final class Auth {
    public static final String SOCIAL_LOGIN_START =
        "소셜 로그인 시작 provider={} stubMode={} hasState={} hasRedirectUri={} hasCodeVerifier={}";
    public static final String SOCIAL_LOGIN_COMPLETE =
        "소셜 로그인 완료 provider={} userId={} newUser={} profileComplete={}";
    public static final String OAUTH_CALLBACK =
        "OAuth 콜백 provider={} hasCode={} hasState={} hasError={}";
    public static final String OAUTH_SUCCESS =
        "OAuth 성공 provider={} userId={} newUser={} profileComplete={}";

    private Auth() {}
  }

  public static final class Batch {
    public static final String TRANSACTION_SYNC =
        "거래 동기화 배치 완료 users={} success={} failed={} savedTotal={}";
    public static final String MYDATA_TOKEN_REFRESH =
        "마이데이터 토큰 갱신 배치 완료 refreshed={} failed={} thresholdHours={}";
    public static final String RECURRING_EXPENSE_LEARN =
        "고정지출 학습 배치 완료 users={} totalLearned={} since={}";
    public static final String FORECAST_ACCURACY = "예측 정확도 배치 완료 forecastDate={} updated={}/{}";
    public static final String MONTHLY_REPORT_KPI =
        "KPI 월간 리포트 userId={} status=SUCCESS signal={} action={}";

    private Batch() {}
  }

  public static final class Cashflow {
    public static final String FORECAST_KPI =
        "KPI 현금흐름 예측 userId={} minBalance={} riskDate={} usedDb={}";
    public static final String RISK_KPI = "KPI 위험 진단 userId={} level={} projectedBalance={}";

    private Cashflow() {}
  }

  public static final class Policy {
    public static final String MATCH_KPI = "KPI 정책 매칭 userId={} matchCount={}";

    private Policy() {}
  }

  public static final class Transaction {
    public static final String SYNC = "거래내역 동기화 userId={} fintechUseNum={} saved={}";

    private Transaction() {}
  }

  public static final class User {
    public static final String PERSONA_INFERRED =
        "페르소나 추론 userId={} occupation={} residence={} income={}";

    private User() {}
  }

  public static final class Notify {
    public static final String STUB_CHANNEL = "[{} 스텁] userId={} title={} body={}";

    private Notify() {}
  }

  public static final class Report {
    public static final String MONTHLY_DELIVERY = "월간 리포트 발송 user={} file={} size={}";
    public static final String HISTORY_STORED = "월간 리포트 이력 저장 user={} month={} status={} detail={}";

    private Report() {}
  }

  public static final class Audit {
    public static final String TRACE = "감사 추적 traceId={} actor={} action={} target={} result={}";

    private Audit() {}
  }

  public static final class Security {
    public static final String CONFIG_MODE =
        "BURTY 보안 모드 적용 mode={} resourceServer={} cors={} swagger={}";

    private Security() {}
  }

  public static final class Jenkins {
    public static final String SECRET_LOADED = "Jenkins 설정 파일(secretKey.json) 로드 완료";

    private Jenkins() {}
  }
}
