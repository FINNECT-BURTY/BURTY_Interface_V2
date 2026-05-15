# BURTY 기능명세서

> **작성일**: 2026-05-09
> **대상**: 기획자 / 디자이너 / PO / 운영팀
> **버전**: v1.0 (현재 구현 기준)
> **베이스 패키지**: `com.burty`
> **기술 스택**: Spring Boot 4.x · Java 21 · MariaDB · Redis · Hibernate(JPA ddl-auto) · WebAuthn4j · JWT · Spring Security · OpenAI(LLM) · SSE

---

## 0. 한눈에 보는 BURTY

BURTY는 **사회초년생(20·30대 청년)** 을 핵심 페르소나로 한 **금융 에이전트** 서비스입니다.

> "월말 적자 반복형 사회초년생"이 마이데이터·오픈뱅킹으로 자산을 연결하면, BURTY가 **30일 현금흐름**을 예측하고 **위험일을 미리 알리며**, **쉬운 말로 행동을 추천**하고, **청년 정책**까지 매칭해 줍니다. 가족 동의 시 **이상 거래는 가족에게 실시간**으로 알립니다.

### 0.1 한눈에 보는 가치

| 사용자가 얻는 것 | BURTY가 하는 일 |
| --- | --- |
| 다음 월급일까지 잔액이 부족한지 미리 본다 | 30일 잔액 예측 + RED/YELLOW/GREEN 신호 |
| 무엇을 줄여야 할지 안다 | Top-1 행동 추천(구독 정리/이체일 조정/임시 한도 등) |
| 청년 혜택을 놓치지 않는다 | 나이·소득·직업·거주지 기반 정책 매칭 |
| 큰돈이 빠져나가는 걸 가족이 안다 | 100만 원 이상 이체는 가족 알림(동의 시) |
| 어렵지 않게 이해한다 | EasyRead(쉬운 말) + 음성(STT/TTS) |
| 안전하게 송금한다 | 3단계 위험 기반 인증(JWT → 단계 인증 → 생체) |

### 0.2 보안 인증 단계 (Risk-based Authentication)

| 레벨 | 명칭 | 설명 | 적용 예시 |
| --- | --- | --- | --- |
| LEVEL_1 | 일반 인증 | JWT 보유 사용자 | 자산 조회, 상담, 알림 조회 |
| LEVEL_2 | 단계 상승 | 한도 변경/민감 설정 | 한도 수정, 가족 동의 등록, 동의 이력 조회 |
| LEVEL_3 | 생체/고위험 | WebAuthn(FIDO2) 또는 Risk Proof | **이체**, 평문 계좌 조회, 동의 철회, 디바이스 해제, 관리자 API |

> 모든 컨트롤러 메서드에 `@AuthLevel(RiskLevel.LEVEL_X)`로 강제됩니다.

---

## 1. 도메인 맵 (기능 모듈 카테고리)

| 카테고리 | 주요 기능 | 핵심 컨트롤러 |
| --- | --- | --- |
| 1. 인증·세션 | 소셜 로그인, JWT, 로그아웃, 세션, 리스크 평가 | `AuthController`, `SessionManagementController`, `LoginRiskController` |
| 2. 생체·디바이스 | WebAuthn 등록·인증, 신뢰 디바이스 관리 | `BurtyController`(security/*), `DeviceManagementController` |
| 3. 동의·연결관리 | 마이데이터/소셜/생체 연결 해제, 동의 이력 | `ConsentManagementController`, `MyDataInstitutionController` |
| 4. 자산·마이데이터 | 자산 요약/추이, 마이데이터 OAuth | `BurtyController`(assets/*, mydata/*) |
| 5. 거래·카테고리 | 거래 동기화, 자동 카테고리, 재분류 | `TransactionController` |
| 6. 현금흐름 예측 | 30일 예측, 위험진단, 캘린더, 위험원인, 기준 입력 | `BurtyController`(cashflow/*), `CashflowManagementController` |
| 7. 행동추천 | Top-1 행동, 실행, 피드백, 추적 | `BurtyController`(action/*), `ActionTrackingController`, `UserFeedbackController` |
| 8. 정책 매칭 | 청년 정책 추천, 신청 표시, 정책 관리(관리자) | `BurtyController`(policy/*), `PolicyAdminController` |
| 9. 페르소나 | 직업/거주/소득 추론·수정 | `PersonaController` |
| 10. 송금 | 내부 이체, 5개 외부은행, 오픈뱅킹 이체, 등록 계좌 | `BurtyController`(transfers/*, external/*), `RegisteredAccountController` |
| 11. 가족 보호 | 가족 동의, 가족 알림(SSE), 가족 대시보드 | `BurtyController`(family/*) |
| 12. 알림 | 위험일/결제일/정책 마감 리마인더 | `NotificationManagementController` |
| 13. 월간 리포트 | 월간 리포트 PDF 생성·발송 | `BurtyController`(reports/*), `MonthlyReportBatchService` |
| 14. AI·음성·상담 | AI 상담, EasyRead, STT/TTS | `BurtyController`(consult/*), `VoiceController` |
| 15. 운영·KPI | 감사 로그, KPI 대시보드, 기준 코드, AI 템플릿 | `AuditLogController`, `KpiDashboardController`, `BaseCodeController`, `AiTemplateAdminController` |
| 16. 시연용 | 데모 세션 | `DemoController` |

---

## 2. 카테고리별 상세 기능 명세

### 2.1 인증·세션

#### 2.1.1 소셜 로그인 / JWT 발급

| 항목 | 내용 |
| --- | --- |
| 기능명 | 소셜 로그인 (GOOGLE / KAKAO / NAVER / APPLE) |
| 사용자 가치 | 별도 회원가입 없이 빠르게 시작 |
| 입력 | provider, code(인가코드), redirectUri, state(OAuth CSRF·`burty.social.stub-mode=false`일 때 서버 검증), codeVerifier(PKCE·선택) |
| 출력 | userId, provider, accessToken(JWT), isNewUser, profileComplete(`tbl_user_profile` 존재 여부) |
| 인증 레벨 | 비로그인(공개) |
| 처리 흐름 | ① 인가 URL 발급(응답에 `state` 포함·서버 보관) → ② 소셜에서 code·state 수신 → ③ 토큰 교환(code_verifier 선택) → ④ 사용자 매핑/생성 → ⑤ JWT 발급 |
| 보안 | CI/전화번호 SHA-256 해시 + AES-GCM 암호화 저장(신규), 레거시 ECB 암호문은 복호화만 호환, 감사 로그 기록 |
| 관련 API | `GET /api/burty/auth/social/{provider}/authorize-url` (응답: authorizeUrl, state)<br>`POST /api/burty/auth/social/{provider}/login`<br>`POST /api/burty/onboarding/profile` (추가 프로필·필수 약관 동의 후 `tbl_consent_record`에 TERMS/PRIVACY 기록)<br>`POST /api/burty/auth/token` (테스트용 JWT — `burty.auth.test-token-enabled=true`이고 `prod` 프로파일이 아닐 때만 허용) |

#### 2.1.2 로그아웃 / 토큰 블랙리스트

| 항목 | 내용 |
| --- | --- |
| 기능명 | 로그아웃 |
| 사용자 가치 | 분실·공용기기 사용 후 즉시 차단 |
| 입력 | Bearer Token |
| 출력 | success: true |
| 인증 레벨 | LEVEL_1 (Bearer) |
| 비고 | 블랙리스트는 `JwtBlacklistService`(Redis 기반)로 관리 |
| 관련 API | `POST /api/burty/auth/logout` |

#### 2.1.3 세션·리프레시 토큰

| 항목 | 내용 |
| --- | --- |
| 기능명 | 기기별 세션·리프레시 발급/조회/만료 |
| 사용자 가치 | 기기별로 로그인 상태 분리, 한 기기에서만 로그아웃 가능 |
| 입력 | userId, deviceId(선택), refreshToken |
| 출력 | accessToken + refreshToken 쌍, 세션 목록 |
| 인증 레벨 | 생성/조회/단건 만료 LEVEL_2, 전체 만료 LEVEL_3 |
| 보안 | refreshToken은 SHA-256 해시 저장, 30일 만료 |
| 관련 API | `POST /api/burty/sessions`<br>`POST /api/burty/sessions/refresh`<br>`GET /api/burty/sessions`<br>`DELETE /api/burty/sessions/{id}`<br>`DELETE /api/burty/sessions` (전체) |

#### 2.1.4 이상 로그인 평가 (Login Risk Evaluation)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 새 기기/심야시간/지역 이탈 등 이상 로그인 감지 |
| 사용자 가치 | 계정 탈취 의심 시 즉시 알림 |
| 위험 사유 | NEW_DEVICE, UNUSUAL_TIME(23–06시), REGION_OUT_OF_SEOUL |
| 위험 등급 | LOW(0건) / MEDIUM(1건) / HIGH(2건+) |
| 후속 동작 | MEDIUM 이상이면 IN-APP 알림 자동 생성 |
| 인증 레벨 | LEVEL_2 |
| 관련 API | `POST /api/burty/security/login-risk/evaluate` |

#### 2.1.5 추가 프로필 등록 (소셜 직후 온보딩)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 소셜 로그인 이후 실명·생년월일·휴대폰·UX 모드 등록 |
| 입력 | phone, name, birthDate, termsAccepted(필수), ageRange·uxMode(선택) |
| 출력 | completed, alreadyRegistered |
| 인증 레벨 | LEVEL_1 (JWT subject = 저장 대상 사용자) |
| 부수효과 | `tbl_consent_record`에 TERMS, PRIVACY 동의 행 생성(버전은 `burty.onboarding.consent.*-version`) |
| 관련 API | `POST /api/burty/onboarding/profile` |

#### 2.1.6 리소스 소유 검증 (IDOR 방지)

| 항목 | 내용 |
| --- | --- |
| 기능명 | URL `userId` 경로변수 또는 쿼리 파라미터가 JWT subject와 일치하는지 검사 |
| 적용 범위 | `/api/burty/**` (단, `/api/burty/auth/**`, `/api/burty/admin/**` 제외) |
| 예외 | `@RequestBody` JSON 내부의 userId는 본 인터셉터 범위 밖 — 추후 `/me` 스타일 API로 이전 권장 |

---

### 2.2 생체·디바이스 (WebAuthn / FIDO2)

#### 2.2.1 WebAuthn 등록·인증

| 항목 | 내용 |
| --- | --- |
| 기능명 | 생체 인증 등록 / 인증 (FIDO2 표준) |
| 사용자 가치 | 비밀번호 없이 지문/얼굴로 안전 송금 |
| 처리 흐름 | begin(challenge 발급) → 단말 서명 → finish(검증) → 신뢰 디바이스로 등록 |
| 결과 | deviceId, deviceToken, accessToken, riskProof(LEVEL_3) |
| 인증 레벨 | LEVEL_1 (challenge 발급/검증) |
| 라이브러리 | `webauthn4j-core` |
| 관련 API | `/api/burty/security/webauthn/register/begin`<br>`/api/burty/security/webauthn/register/finish`<br>`/api/burty/security/webauthn/authenticate/begin`<br>`/api/burty/security/webauthn/authenticate/finish` |

#### 2.2.2 신뢰 디바이스 관리

| 항목 | 내용 |
| --- | --- |
| 기능명 | 등록된 기기 목록/이름 변경/해제 |
| 사용자 가치 | 분실/교체 시 즉시 그 기기에서 로그인 차단 |
| 출력 필드 | deviceId, deviceName, platform(IOS/ANDROID/WEB), osVersion, appVersion, isTrusted, lastSeenAt, createdAt |
| 인증 레벨 | 조회/이름변경 LEVEL_2, 해제 LEVEL_3 |
| 부수효과 | 기기 해제 시 그 기기에 묶인 모든 생체 credential도 즉시 폐기 |
| 관련 API | `GET /api/burty/devices`<br>`PATCH /api/burty/devices/{id}/name`<br>`DELETE /api/burty/devices/{id}` |

#### 2.2.3 Risk Proof (LEVEL_2 → LEVEL_3 단계 상승)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 단계 인증 증표 발급 |
| 사용자 가치 | 한 번 인증 후 이체 등 LEVEL_3 동작을 짧은 시간 내 수행 |
| 인증 레벨 | LEVEL_1 (요청 자체) → LEVEL_3 증표 발급 |
| 관련 API | `POST /api/burty/security/level2/proof` |

---

### 2.3 동의·연결관리

#### 2.3.1 동의 이력 / 철회

| 항목 | 내용 |
| --- | --- |
| 기능명 | 개인정보·마이데이터·위치·보안 동의 이력 조회/철회 |
| 사용자 가치 | "내가 어디까지 동의했는지" 한 화면에서 확인·철회 |
| 출력 | consentId, consentType, version, agreedAt, revokedAt |
| 인증 레벨 | 조회 LEVEL_2, 철회 LEVEL_3 |
| 관련 API | `GET /api/burty/consents`<br>`POST /api/burty/consents/{id}/revoke` |

#### 2.3.2 마이데이터 기관 다중 연동

| 항목 | 내용 |
| --- | --- |
| 기능명 | 여러 금융기관 마이데이터 OAuth 연결 / 해제 |
| 사용자 가치 | 은행/카드/증권 등 기관별로 따로 연결·해제 |
| 상태값 | ACTIVE, FAILED, UNLINKED + tokenExpiresAt, lastErrorCode |
| 인증 레벨 | 인가/콜백 LEVEL_1, 해제 LEVEL_2/LEVEL_3(보안 옵션) |
| 관련 API | `GET /api/burty/mydata/institutions`<br>`GET /api/burty/mydata/institutions/{code}/authorize`<br>`POST /api/burty/mydata/institutions/{code}/callback`<br>`DELETE /api/burty/mydata/institutions/{code}` |

#### 2.3.3 소셜·생체 연결 해제

| 항목 | 내용 |
| --- | --- |
| 기능명 | 소셜 로그인 연결 해제 / 생체 인증 일괄 해제 |
| 인증 레벨 | LEVEL_3 |
| 관련 API | `DELETE /api/burty/consents/social/{provider}`<br>`DELETE /api/burty/consents/biometric` |

---

### 2.4 자산·마이데이터

#### 2.4.1 자산 요약 / 추이

| 항목 | 내용 |
| --- | --- |
| 기능명 | 자산 스냅샷 요약, 시간별 변동 추이 |
| 사용자 가치 | 내 총 자산이 늘었는지 줄었는지 한눈에 |
| 출력 | totalAsset, totalDebt, netWorth, volatilityPercent, 일별 추이 |
| 데이터 소스 | 마이데이터 Port → 폴백 시 추정 |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/assets/summary`<br>`GET /api/burty/assets/trend` |

---

### 2.5 거래·카테고리

#### 2.5.1 오픈뱅킹 거래 동기화

| 항목 | 내용 |
| --- | --- |
| 기능명 | fintechUseNum 단위 거래내역 적재 + 자동 분류 |
| 사용자 가치 | 카드/계좌 거래가 자동으로 카테고리화되어 들어옴 |
| 입력 | userId, fintechUseNum |
| 출력 | saved(저장 건수) |
| 분류 엔진 | `TransactionCategorizer` (CategoryRule 우선순위 + 가맹점 키워드) |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `POST /api/burty/transactions/sync` |

#### 2.5.2 거래내역 조회

| 항목 | 내용 |
| --- | --- |
| 기능명 | 기간별 거래 조회 (기본 최근 3개월) |
| 출력 | txId, txnDate, amount, direction(IN/OUT), merchant, memo, expenseCategoryCode, incomeCategoryCode, source, categoryConfidence |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/transactions?from=&to=` |

#### 2.5.3 전체 재분류

| 항목 | 내용 |
| --- | --- |
| 기능명 | 카테고리 룰 변경 후 전체 거래 재분류 |
| 인증 레벨 | LEVEL_2 |
| 관련 API | `POST /api/burty/transactions/recategorize` |

---

### 2.6 현금흐름 예측 ⭐핵심

#### 2.6.1 30일 현금흐름 예측

| 항목 | 내용 |
| --- | --- |
| 기능명 | 향후 30일 일별 잔액·위험일 예측 |
| 사용자 가치 | "이번 달 며칠에 잔액이 부족할지" 미리 알 수 있음 |
| 입력 | userId |
| 출력 | generatedDate, openingBalance, minimumBalance, riskDate, riskReason, dailyBalances[], safetyBalance, dataSource(MYDATA_PLUS_CUSTOM_CRITERIA / MYDATA_FALLBACK_ESTIMATE), customCriteriaUsed |
| 계산 입력 | 마이데이터 자산 + 고정수입/지출 일정(`tbl_cashflow_schedule`) + 반복지출(`tbl_recurring_expense`) + 사용자 안전잔액·시작잔액·변동지출 예산(`tbl_user_setting`) |
| 결과 저장 | `tbl_cashflow_forecast` 히스토리에 스냅샷 |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/cashflow/forecast` |

#### 2.6.2 위험 진단 (RED/YELLOW/GREEN)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 30일 최저 잔액 기준 위험 등급 진단 |
| 등급 기준 | RED: 최저잔액 < 0 / YELLOW: < 안전잔액(또는 코드 기준 5만원) / GREEN: 그 이상 |
| 출력 | level, threshold, reason, riskDate, projectedBalance |
| 결과 저장 | `tbl_risk_assessment` 히스토리 |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/cashflow/risk` |

#### 2.6.3 현금흐름 캘린더

| 항목 | 내용 |
| --- | --- |
| 기능명 | 월력 위에 일별 잔액 + 결제·급여 이벤트 표시 |
| 사용자 가치 | 달력에서 "위험일"을 시각적으로 인지 |
| 출력 | 일별 [date, balance, isRisk, events[] (월세/카드/대출/급여/구독 등)] |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/cashflow-management/calendar` |

#### 2.6.4 위험 원인 분해

| 항목 | 내용 |
| --- | --- |
| 기능명 | "왜 위험한가?"를 카테고리별 금액 영향도로 분해 |
| 카테고리 | RENT(월세), CARD_BILL(카드값), LOAN(대출), UTILITY(공과금), SUBSCRIPTION(구독), VARIABLE_SPEND(변동지출), FIXED_EXPENSE(기타 고정) |
| 출력 | causeType, label, impactAmount, description |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/cashflow-management/risk-causes` |

#### 2.6.5 사용자 직접 입력 기준 (커스텀 기준)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 안전잔액·현재잔액 직접입력·월 변동지출 예산 저장 |
| 사용자 가치 | 마이데이터가 정확하지 않을 때 직접 보정 |
| 키 | SAFETY_BALANCE, OPENING_BALANCE_OVERRIDE, MONTHLY_VARIABLE_BUDGET |
| 인증 레벨 | 저장 LEVEL_2, 조회 LEVEL_1 |
| 관련 API | `POST /api/burty/cashflow/criteria`<br>`GET /api/burty/cashflow/criteria` |

#### 2.6.6 고정 일정 등록 / 자동 인식 고정지출

| 항목 | 내용 |
| --- | --- |
| 기능명 | 월세/관리비/통신비/구독/대출 직접 등록, 거래내역 학습으로 자동 인식 |
| 자동 학습 | 월 1회 배치(`RecurringExpenseLearnBatch`)가 3개월간 3회 이상·표준편차 35% 이내 항목을 학습 |
| 출력 | scheduleId/expenseId, label, amount, dayOfMonth, direction, active |
| 인증 레벨 | 등록/비활성 LEVEL_2, 조회 LEVEL_1 |
| 관련 API | `GET/POST/DELETE /api/burty/cashflow-management/schedules`<br>`GET /api/burty/cashflow/recurring-expenses` |

---

### 2.7 행동 추천 ⭐핵심

#### 2.7.1 Top-1 행동 추천

| 항목 | 내용 |
| --- | --- |
| 기능명 | 사용자별 최우선 행동 1개 추천 |
| 사용자 가치 | "지금 무엇부터 해야 할지" 지시받음 |
| 추천 후보 | 추천 카탈로그(`tbl_action_recommendation`)에서 잔액·직업 매칭 후보군 → 베이스 점수 + 피드백 점수 + 직업 부스트 적용 → Bandit 알고리즘(`BanditScorer`)으로 Top-1 선택 |
| 출력 | actionType, title, description(EasyRead), estimatedImprovement, priorityScore, advisoryBoundary |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/cashflow/action` |

#### 2.7.2 행동 실행

| 항목 | 내용 |
| --- | --- |
| 기능명 | 추천 행동 실행 처리 (실행 이력 저장) |
| 인증 레벨 | LEVEL_2 |
| 관련 API | `POST /api/burty/cashflow/action/execute` |

#### 2.7.3 행동 피드백 / 효과 추적

| 항목 | 내용 |
| --- | --- |
| 기능명 | 추천 수락/거절, 실행 효과 추적 |
| 사용자 가치 | 피드백이 다음 추천에 반영되어 점점 정밀해짐 |
| 출력(추적) | acceptedCount, rejectedCount, executedCount, projectedBalance, riskLevel |
| 인증 레벨 | 피드백 LEVEL_1, 추적 LEVEL_1 |
| 관련 API | `POST /api/burty/cashflow/action/feedback`<br>`GET /api/burty/cashflow/action/feedback-summary`<br>`GET /api/burty/actions/tracking/{actionType}` |

#### 2.7.4 일반 사용자 피드백

| 항목 | 내용 |
| --- | --- |
| 기능명 | "도움이 되었나요?" / 금액 정확도 / 고정비 여부 등 일반 피드백 |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `POST /api/burty/feedback` |

---

### 2.8 정책 매칭

#### 2.8.1 청년 정책 매칭

| 항목 | 내용 |
| --- | --- |
| 기능명 | 사용자 페르소나 기반 정부·지자체 정책 추천 |
| 매칭 조건 | 나이(min/max), 소득(max), 직업(NEW_WORKER/STUDENT 등), 거주지, 유효기간 |
| 우선순위 | priorityBase + 신청률 부스트 + 매칭 정밀도 |
| 출력 | policyCode, policyName, supportType, reason, priorityScore (Top-3) |
| 사용자 가치 | 청년·신혼·자영업·구직자 등 본인에게 맞는 정책만 추려서 보여줌 |
| 결과 저장 | `tbl_policy_match_log` |
| 인증 레벨 | 매칭 LEVEL_1, 신청 표시 LEVEL_2 |
| 관련 API | `GET /api/burty/policy/match`<br>`POST /api/burty/policy/{policyCode}/apply` |

#### 2.8.2 정책 카탈로그 관리 (관리자)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 정책 등록/수정/비활성, 신청 URL 관리 |
| 인증 레벨 | LEVEL_3 |
| 관련 API | `GET/POST/DELETE /api/burty/admin/policies` |

---

### 2.9 페르소나

| 항목 | 내용 |
| --- | --- |
| 기능명 | 사용자 페르소나 자동 추론 / 사용자 직접 수정 / 재추론 |
| 사용자 가치 | 내 상황에 맞는 추천을 받기 위해 직업/거주/세대/소득을 한 곳에서 관리 |
| 추론 입력 | 자산 스냅샷 + 거래 패턴 |
| 필드 | occupationCode(NEW_WORKER/STUDENT/JOB_SEEKER/FREELANCER/PART_TIMER), residenceCode(MONTHLY_RENT 등), householdType(SINGLE 등), monthlyIncomeAvg, incomeVariabilityPct, age, source(SYSTEM/USER), userOverridden |
| 인증 레벨 | 조회 LEVEL_1, 수정/재추론 LEVEL_2 |
| 관련 API | `GET /api/burty/persona/{userId}`<br>`PUT /api/burty/persona/{userId}`<br>`POST /api/burty/persona/{userId}/reinfer` |

---

### 2.10 송금

#### 2.10.1 BURTY 내부 이체

| 항목 | 내용 |
| --- | --- |
| 기능명 | 이체 요청 (가족 알림·이상거래·한도·생체 검증 통합) |
| 입력 | userId, fromAccount, toAccount, amount, description, assertionToken(생체) |
| 출력 | transferId, status(REQUESTED/COMPLETED/BLOCKED), familyNotified |
| 핵심 정책 | • 1회 한도 사용자 설정값 검사<br>• 100만 원 이상 → 가족 알림(동의 시)<br>• 300만 원 이상 → 대형이체 룰 트리거 |
| 인증 레벨 | LEVEL_3 (생체) |
| 관련 API | `POST /api/burty/transfers`<br>`GET /api/burty/transfers/{id}`<br>`GET /api/burty/transfers?userId=` |

#### 2.10.2 외부 은행 이체 (5개 은행 + 오픈뱅킹)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 카카오뱅크/하나/KB/신한/iM 직결 이체 + 일반 오픈뱅킹 이체 |
| 인증 레벨 | LEVEL_3 |
| 관련 API | `POST /api/burty/external/{kakao-bank|hana-bank|kb-bank|shinhan-bank|im-bank}/transfer`<br>`POST /api/burty/external/openbanking/transfer` |
| 부가 조회 | `GET /api/burty/external/openbanking/{accounts|balance|transactions}`<br>`GET /api/burty/external/pension/summary` |

#### 2.10.3 등록 계좌 (PII 강화)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 자주 보내는 계좌 등록·해제 |
| 보안 설계 | 계좌번호를 ① **해시(검색용)** ② **AES-GCM 암호화(보관)** ③ **마스킹(표시용)** 3중 컬럼으로 분리 저장 |
| 인증 레벨 | 등록/해제 LEVEL_2, 마스킹 조회 LEVEL_1, **평문 조회 LEVEL_3** |
| 관련 API | `POST /api/burty/registered-accounts`<br>`GET /api/burty/registered-accounts`<br>`GET /api/burty/registered-accounts/decrypted` (LEVEL_3)<br>`DELETE /api/burty/registered-accounts` |

---

### 2.11 가족 보호

#### 2.11.1 가족 동의

| 항목 | 내용 |
| --- | --- |
| 기능명 | 자녀-부모 가족 연결 동의 등록/수정/철회/조회 |
| 사용자 가치 | 동의 후에만 가족이 이상 거래·월간 리포트를 받아볼 수 있음 |
| 인증 레벨 | 조회 LEVEL_1, 등록/수정/철회 LEVEL_2 |
| 관련 API | `POST/PATCH/DELETE/GET /api/burty/family/consents` |

#### 2.11.2 가족 알림 (실시간 SSE)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 큰 금액 이체·이상 거래 발생 시 가족에게 푸시 + SSE 스트림 |
| 트리거 | 100만 원 이상 송금, 가족 동의 ON일 때 |
| 출력(스트림) | userId, message, sentAt |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/family-alerts?userId=`<br>`GET /api/burty/family-alerts/stream?userId=` (SSE) |

#### 2.11.3 가족 대시보드

| 항목 | 내용 |
| --- | --- |
| 기능명 | 가족이 보는 자녀 요약 (알림수, 이상거래수, 월간 리포트 발송 횟수) |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `GET /api/burty/family/dashboard?userId=` |

---

### 2.12 알림

| 항목 | 내용 |
| --- | --- |
| 기능명 | 위험일 D-7/D-3/D-1/D-0, 카드/월세 결제일 D-3 이내, 정책 마감 D-7 이내 알림 자동 생성 |
| 알림 종류 | CASHFLOW_RISK, CARD_DUE, RENT_DUE, POLICY_DEADLINE, UNUSUAL_LOGIN |
| 채널 | IN_APP (확장 시 PUSH/SMS/EMAIL) |
| 상태 | QUEUED → SENT → READ |
| 출력 | notificationId, type, channel, title, body, deepLink, status, sentAt, readAt |
| 인증 레벨 | 조회 LEVEL_1, 생성 LEVEL_2 |
| 관련 API | `GET /api/burty/notifications?userId=`<br>`POST /api/burty/notifications/generate-reminders` |

---

### 2.13 월간 리포트

| 항목 | 내용 |
| --- | --- |
| 기능명 | 한 달치 자산·지출 패턴·신호색 요약 PDF 생성·발송 |
| 사용자 가치 | "이번 달 살림"이 한 장 PDF로 정리됨 |
| 자동화 | `MonthlyReportBatchService` 매월 1일 09시 cron으로 전체 사용자 발송 (`burty.report.cron` 설정) |
| 출력 | userId, month, easyReadSummary, signalColor, primaryAction, keyPoints[] |
| 보안 | PDF 암호화(AES-128) 옵션 가능, 발송 이력 `MonthlyReportHistoryPort` 저장 |
| 인증 레벨 | 조회 LEVEL_1 |
| 관련 API | `GET /api/burty/reports/monthly?userId=` |

---

### 2.14 AI · 음성 · 상담

#### 2.14.1 AI 상담 (OpenAI)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 자산·페르소나·위험을 입력으로 LLM이 답변 → EasyRead로 변환 |
| 사용자 가치 | 어려운 금융 용어 없이 내 상황에 맞춘 조언 |
| 입력 | userId, question |
| 출력 | summary(EasyRead), signalColor, recommendedActions[] |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `POST /api/burty/ai/consult` |

#### 2.14.2 룰 기반 상담 (폴백)

| 항목 | 내용 |
| --- | --- |
| 기능명 | LLM 미사용 시 룰 기반 상담 결과 반환 |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `POST /api/burty/consult` |

#### 2.14.3 음성 (STT / TTS)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 음성 → 텍스트 / 텍스트 → 음성 |
| 사용자 가치 | 디지털 약자(시니어/시각약자/이동중)도 사용 가능 |
| 입력 | STT: audioBase64 / TTS: text |
| 출력 | STT: text / TTS: audioUrl |
| 인증 레벨 | LEVEL_1 |
| 관련 API | `POST /api/burty/voice/stt`, `POST /api/burty/voice/tts` |

#### 2.14.4 EasyRead (쉬운 말 변환)

| 항목 | 내용 |
| --- | --- |
| 기능명 | 행동 설명·AI 답변·리포트 요약을 디지털 약자 친화 문체로 변환 |
| 적용 위치 | 행동 추천 description, AI 상담 summary, 월간 리포트 easyReadSummary |
| 비고 | 어떤 컨트롤러도 직접 노출하지 않고 내부에서 자동 적용 |

---

### 2.15 운영 · KPI

#### 2.15.1 감사 로그

| 항목 | 내용 |
| --- | --- |
| 기능명 | 로그인/생체/마이데이터/정책/추천 실행 등 모든 핵심 행위 감사 |
| 출력 | auditId, occurredAt, actorType, action, result, targetType, metadata |
| 인증 레벨 | LEVEL_3 |
| 관련 API | `GET /api/burty/audit-logs?size=` |

#### 2.15.2 KPI 대시보드

| 항목 | 내용 |
| --- | --- |
| 기능명 | 사용자별/전체 KPI 지표 |
| 사용자 KPI | 행동 채택률, 예측 정확도, 위험단계 분포, 점수 Top5 |
| 글로벌 KPI | 관리자용 카운트 (전체 사용자 위험 분포 등) |
| 인증 레벨 | 사용자 LEVEL_1, 글로벌 LEVEL_3 |
| 관련 API | `GET /api/burty/kpi/user/{userId}`<br>`GET /api/burty/kpi/global` |

#### 2.15.3 기준 코드(BaseCode) 관리

| 항목 | 내용 |
| --- | --- |
| 기능명 | 코드 그룹 조회, 단일 코드, 하위 코드, 등록/수정/비활성, 캐시 reload |
| 사용자 가치 | 정책/카테고리/직업/룰 등 기준값 통합 관리 |
| 인증 레벨 | 조회 LEVEL_1, 등록/수정/비활성/reload LEVEL_3 |
| 관련 API | `/api/burty/codes` 계열 |

#### 2.15.4 AI Fallback 템플릿 관리

| 항목 | 내용 |
| --- | --- |
| 기능명 | LLM 사용 불가 시 사용할 fallback 문구 관리 |
| 키 | templateKey, riskLevel, occupationCode, causeType, templateText, active |
| 인증 레벨 | LEVEL_3 |
| 관련 API | `/api/burty/admin/ai-templates` |

---

### 2.16 시연용

| 항목 | 내용 |
| --- | --- |
| 기능명 | 시연용 데모 사용자 + 페르소나 + 현금흐름 시드 데이터 + JWT 자동 발급 |
| 시나리오 | "월말 적자 반복형 사회초년생 직장인 / 잔액 61만원 / 카드값 52만원 D-7 / 월급 14일 후" |
| 인증 레벨 | 비로그인 |
| 관련 API | `POST /api/burty/auth/demo/session` |

---

## 3. 데이터 모델 (핵심 테이블)

> 전체 ERD는 `burty-ERD/burty_Table_Ver1.1.sql` 참조. 기획자가 알면 좋은 핵심 테이블만 발췌.

### 3.1 사용자·인증

| 테이블 | 설명 | 핵심 컬럼 |
| --- | --- | --- |
| `tbl_user` | 사용자 | user_id(UUID), ci_hash, ci_encrypted, phone_hash, phone_encrypted, status, failed_login_count |
| `tbl_user_profile` | 프로필 | user_id, name, age, region |
| `tbl_session` | 세션 | session_id, user_id, device_id, refresh_token_hash, expires_at, revoked_at |
| `tbl_device` | 기기 | device_id, user_id, device_fingerprint, platform, is_trusted, revoked_at |
| `tbl_biometric_credential` | 생체 인증 | credential_id, user_id, device_id, public_key, sign_count, revoked_at |
| `tbl_social_account` | 소셜 계정 | provider, social_id, user_id |
| `tbl_consent_record` / `tbl_consent_document` | 동의 이력 / 문서 | consent_id, consent_type, version, agreed_at, revoked_at |

### 3.2 자산·거래·현금흐름

| 테이블 | 설명 |
| --- | --- |
| `tbl_linked_institution` / `tbl_mydata_link_status` | 마이데이터 연결 기관/상태 |
| `tbl_account` / `tbl_account_snapshot` | 계좌 / 잔액 스냅샷 |
| `tbl_transaction` | 거래내역(원시+카테고리) |
| `tbl_category_rule` | 거래 분류 룰 |
| `tbl_cashflow_schedule` | 사용자 등록 고정 일정 (월세/카드/대출) |
| `tbl_recurring_expense` | 자동 학습된 반복 지출 |
| `tbl_cashflow_forecast` | 예측 결과 히스토리 |
| `tbl_risk_assessment` | 위험 진단 히스토리 |
| `tbl_income_pattern` | 소득 패턴 |
| `tbl_user_setting` | 사용자 직접 입력 기준 (안전잔액 등) |

### 3.3 송금·이상거래

| 테이블 | 설명 |
| --- | --- |
| `tbl_transfer_order` / `tbl_transfer_event` / `tbl_transfer_failure` | 이체 주문/이벤트/실패 |
| `tbl_transfer_record` | 이체 기록 (가족 알림 트리거 기준) |
| `tbl_daily_transfer_usage` | 일일 한도 사용량 |
| `tbl_fraud_rule` / `tbl_fraud_detection_log` | 이상거래 룰/탐지 로그 |
| `tbl_outbox_event` | 이벤트 발행 Outbox |
| `tbl_registered_account` | 등록 계좌 (hash + encrypted + masked 3중) |

### 3.4 추천·정책·페르소나·가족

| 테이블 | 설명 |
| --- | --- |
| `tbl_persona_profile` | 사용자 페르소나 |
| `tbl_action_recommendation` | 추천 카탈로그 |
| `tbl_action_feedback_score` | 사용자×행동 누적 피드백 점수 |
| `tbl_policy` / `tbl_policy_match_log` | 정책 / 매칭 로그 |
| `tbl_family_consent` | 가족 동의 |
| `tbl_guardian_link` / `tbl_alert_subscription` | 보호자 링크 / 알림 구독 |

### 3.5 운영·로그

| 테이블 | 설명 |
| --- | --- |
| `tbl_audit_log` | 감사 로그 |
| `tbl_notification` | 알림 큐 |
| `tbl_monthly_report` / `tbl_report_section` | 월간 리포트 |
| `tbl_code` | 기준 정보(룩업 카탈로그) |
| `tbl_ai_fallback_template` | AI Fallback 문구 |
| `tbl_encryption_metadata` | KMS 키 버전 메타 |

---

## 4. 주요 비즈니스 룰 한눈에

| 룰 | 임계값/정책 | 위치 |
| --- | --- | --- |
| 가족 알림 트리거 | 1회 송금 ≥ 1,000,000원 + 가족 동의 ON | `BurtyService.FAMILY_ALERT_THRESHOLD` |
| 대형 이체 임계 | 1회 송금 ≥ 3,000,000원 → 이상거래 룰 진입 | `BurtyService.LARGE_TRANSFER_THRESHOLD` |
| YELLOW 위험 임계 | 30일 최저잔액 < 안전잔액(기본 5만원) | `RiskAssessmentService.DEFAULT_LOW` |
| RED 위험 임계 | 30일 최저잔액 < 0원 | `RiskAssessmentService.DEFAULT_NEGATIVE` |
| 반복지출 학습 | 3개월 내 3회 이상 + 표준편차 ≤ 35% | `RecurringExpenseLearnBatch` |
| 이상 로그인 등급 | 0건=LOW · 1건=MEDIUM · 2건+=HIGH | `LoginRiskController` |
| 위험일 알림 | D-7 / D-3 / D-1 / D-0 | `NotificationManagementController.generateReminders` |
| 결제일 알림 | D-3 이내 카드/월세 일정 | 〃 |
| 정책 마감 알림 | 마감까지 0–7일 | 〃 |
| 월간 리포트 발송 | 매월 1일 09:00 | `MonthlyReportBatchService` (`burty.report.cron`) |
| 추천 점수 | 베이스 + 피드백 점수 + 직업 부스트 + Bandit 선택 | `ActionRecommendationService` |
| 정책 매칭 | 나이/소득/직업/거주/유효기간 + 신청률 부스트 → Top-3 | `PolicyMatchingService` |

---

## 5. 보안·컴플라이언스 요약

| 영역 | 적용 내용 |
| --- | --- |
| 식별정보 | CI / 전화번호 → SHA-256 해시(검색) + AES-GCM 암호화(보관) |
| 계좌번호 | 해시 / AES-GCM 암호화 / 마스킹 3중 분리 |
| 토큰 | JWT(jjwt 0.12.6), refreshToken은 SHA-256 해시 저장, 30일 만료 |
| 토큰 폐기 | Redis 블랙리스트 |
| 생체 | WebAuthn4j(FIDO2), 신뢰 디바이스 모델 |
| 위험 인증 | LEVEL_1 / LEVEL_2 / LEVEL_3 강제 (`@AuthLevel` 인터셉터) |
| 동의 관리 | 동의 이력 저장 + 철회 시 LEVEL_3 + 마이데이터/소셜/생체 연결 해제 연동 |
| 감사 로그 | 로그인/생체/마이데이터/정책/추천 실행 기록 |
| KMS 메타 | `tbl_encryption_metadata`로 키 버전 관리 |
| 마이그레이션 | Hibernate `spring.jpa.hibernate.ddl-auto`(기본 `update`; Flyway 미사용) |
| 외부 통신 | WebClient (Spring WebFlux) |
| API 문서 | Springdoc OpenAPI 2.8.9 → `/swagger-ui` |

---

## 6. 운영 정보

| 항목 | 내용 |
| --- | --- |
| 빌드 | Gradle, Java 21 |
| Spring Boot | 4.0.5 |
| DB | MariaDB (운영) / H2 (테스트) |
| 캐시·세션 | Redis |
| 인프라 파일 | `Dockerfile`, `docker-compose.yml`, `Jenkinsfile` |
| 배치 스케줄 | 월간 리포트(매월 1일 09시), 반복지출 학습(매월 1일 03시), 마이데이터 토큰 리프레시, 거래 동기화, 예측 정확도 검증 |
| 인터셉터 | `AuthLevelInterceptor` — `@AuthLevel` 메서드 RiskLevel 검사 |
| API 인증 | `JwtAuthenticationFilter` |
| 외부 포트 | `MyDataPort`, `OpenBankingPort`, `MyDataOAuthPort`, `LlmPort`, `EasyReadPort`, `BiometricAuthPort`, `FamilyAlertPort`, `AuditLogPort`, `MonthlyReportDeliveryPort`, `MonthlyReportHistoryPort` |

---

## 7. 페르소나 시나리오 (기획 참고)

### 7.1 사회초년생 직장인 (NEW_WORKER)

> "입사 6개월차, 월급 245만원, 월세 75만원, 카드값 변동 큰 사용자"

| 시점 | BURTY가 보여주는 화면 |
| --- | --- |
| 카드값 D-7 | 알림 "카드 결제일이 다가와요 520,000원" |
| D-3 | "현금흐름 위험일이 다가와요" + 캘린더에 RED 마커 |
| D-3 | Top-1 행동: "월세 자동이체일 1일 늦추기" (예상 개선 +120,000원) |
| D-1 | 가족 알림(부모 동의 ON 시) "이체 850,000원 발생" |
| 다음 달 1일 | 월간 리포트 PDF 자동 발송 |

### 7.2 학생/구직자 (STUDENT / JOB_SEEKER)

| 시점 | 화면 |
| --- | --- |
| 첫 진입 | 페르소나 자동 추론 → "학생/소득변동 큰" 분류 |
| 정책 매칭 | "청년 월세 지원" / "구직 활동 지원금" Top-3 |
| 페르소나 직접 수정 | "직업: STUDENT" 명시 → 추천 점수 부스트 |

### 7.3 가족 동의 켜진 자녀

| 시점 | 화면 |
| --- | --- |
| 100만원 이상 송금 | 부모 앱에 SSE 실시간 알림 |
| 매월 1일 | 부모 앱 가족 대시보드에 자녀 요약 갱신 |

---

## 8. API 카탈로그 (기능 → URL 빠른 인덱스)

| 카테고리 | 메서드 | 경로 |
| --- | --- | --- |
| 인증 | POST | `/api/burty/auth/token` (테스트 전용, `burty.auth.test-token-enabled`·`prod` 제한) |
| 인증 | GET | `/api/burty/auth/social/{provider}/authorize-url` (응답: authorizeUrl, state) |
| 인증 | POST | `/api/burty/auth/social/{provider}/login` |
| 인증 | POST | `/api/burty/onboarding/profile` |
| 인증 | POST | `/api/burty/auth/logout` |
| 인증 | POST | `/api/burty/auth/demo/session` |
| 세션 | POST/GET/DELETE | `/api/burty/sessions[/{id}]` |
| 세션 | POST | `/api/burty/sessions/refresh` |
| 보안 | POST | `/api/burty/security/login-risk/evaluate` |
| 보안 | POST | `/api/burty/security/level2/proof` |
| 생체 | POST | `/api/burty/security/webauthn/{register|authenticate}/{begin|finish}` |
| 디바이스 | GET/PATCH/DELETE | `/api/burty/devices[/{id}[/name]]` |
| 동의 | GET/POST | `/api/burty/consents[/{id}/revoke]` |
| 동의 | DELETE | `/api/burty/consents/{mydata|social|biometric}/...` |
| 마이데이터 | GET/POST/DELETE | `/api/burty/mydata/institutions/...` |
| 자산 | GET | `/api/burty/assets/{summary|trend}` |
| 거래 | GET/POST | `/api/burty/transactions[/sync|/recategorize]` |
| 현금흐름 | GET | `/api/burty/cashflow/{forecast|risk|action|recurring-expenses|criteria}` |
| 현금흐름 | POST | `/api/burty/cashflow/{criteria|action/execute|action/feedback}` |
| 현금흐름 | GET | `/api/burty/cashflow/action/feedback-summary` |
| 캘린더 | GET/POST/DELETE | `/api/burty/cashflow-management/{calendar|schedules|risk-causes}` |
| 추천 추적 | GET | `/api/burty/actions/tracking/{actionType}` |
| 피드백 | POST | `/api/burty/feedback` |
| 정책 | GET/POST | `/api/burty/policy/{match|{code}/apply}` |
| 정책(관리) | GET/POST/DELETE | `/api/burty/admin/policies[/{code}]` |
| 페르소나 | GET/PUT/POST | `/api/burty/persona/{userId}[/reinfer]` |
| 송금 | POST/GET | `/api/burty/transfers[/{id}]` |
| 외부은행 | POST/GET | `/api/burty/external/{kakao-bank|hana-bank|kb-bank|shinhan-bank|im-bank|openbanking|pension}/...` |
| 등록계좌 | POST/GET/DELETE | `/api/burty/registered-accounts[/decrypted]` |
| 가족 | POST/PATCH/DELETE/GET | `/api/burty/family/consents` |
| 가족 | GET | `/api/burty/family-alerts[/stream]` |
| 가족 | GET | `/api/burty/family/dashboard` |
| 알림 | GET/POST | `/api/burty/notifications[/generate-reminders]` |
| 리포트 | GET | `/api/burty/reports/monthly` |
| AI/상담 | POST | `/api/burty/{consult|ai/consult}` |
| 음성 | POST | `/api/burty/voice/{stt|tts}` |
| 운영 | GET | `/api/burty/audit-logs` |
| 운영 | GET | `/api/burty/kpi/{user/{id}|global}` |
| 운영 | GET/POST/DELETE | `/api/burty/codes/...` |
| 운영 | GET/POST/DELETE | `/api/burty/admin/ai-templates[/{key}]` |

---

## 9. 향후 확장 포인트 (현재 구조에서 손쉬운 다음 단계)

| 영역 | 다음 단계 제안 |
| --- | --- |
| 알림 채널 | IN_APP 외 PUSH/SMS/EMAIL 어댑터 추가 (현재 enum만 정의됨) |
| 추천 모델 | Bandit → Contextual Bandit / 실험군별 A/B |
| 정책 데이터 | 외부 정책 카탈로그 자동 수집 (현재 수동 등록) |
| AI 캐시 | LLM 응답 캐시·비용 가드 |
| 가족 채널 | SSE → FCM/APNs 멀티 채널 |
| 마이데이터 | 토큰 리프레시 실패 시 사용자 재인가 가이드 UX |
| 모니터링 | KPI 로그를 Grafana/Prometheus로 적재 |

---

> 본 문서는 현재 소스 트리(`src/main/java/com/burty/**`)와 ERD(`burty-ERD/burty_Table_Ver1.1.sql`)를 기준으로 작성되었습니다. 컨트롤러 추가/변경 시 본 문서의 §2 / §8을 함께 갱신해 주세요.