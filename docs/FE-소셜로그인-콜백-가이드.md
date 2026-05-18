# 소셜 로그인 콜백 — 프론트엔드 연동 가이드

대상: FE 팀
관련 BE 코드:
- `src/main/java/com/burty/adapter/in/web/support/SocialAuthSupport.java`
- `src/main/java/com/burty/adapter/in/web/{Kakao,Google,Naver,Apple}AuthController.java`
- `src/main/java/com/burty/security/{AuthCookies,JwtAuthenticationFilter}.java`
- `src/main/java/com/burty/config/BurtyAuthProperties.java`

---

## 1. 유저 토큰을 어떻게 받는가

**결론: FE는 토큰을 직접 받지 않는다. BE가 HttpOnly 쿠키로 심어주고, 이후 브라우저가 자동 전송한다.**

### 전체 흐름 (BFF 패턴)

```
[FE] GET /auth/kakao/authorize-url
       └─► { authorizeUrl, state } 응답
[FE] window.location = authorizeUrl
       └─► 카카오 로그인 화면

[Kakao] GET https://api.burty.co.kr/auth/kakao/callback?code=...&state=...
       └─► (BE 가 직접 받음, FE 거치지 않음)

[BE] code 교환 → access/refresh JWT 발급
       └─► Set-Cookie: BURTY_ACCESS=<jwt>;  HttpOnly; Secure; SameSite=Lax
       └─► Set-Cookie: BURTY_REFRESH=<opaque>; HttpOnly; Secure; SameSite=Lax
       └─► 302 Location: https://burty.co.kr/auth/callback?newUser=true&profileComplete=false

[FE] /auth/callback 페이지 로드
       └─► URL 쿼리 파라미터만 읽음 (error / newUser / profileComplete)
       └─► 분기 처리 후 라우팅
       └─► 이후 fetch / axios 호출 시 credentials: 'include' 만 켜면 쿠키 자동 전송됨
```

### 핵심 포인트

| 항목 | 값 |
|---|---|
| Access 쿠키명 | `BURTY_ACCESS` (JWT) |
| Refresh 쿠키명 | `BURTY_REFRESH` (opaque token) |
| 속성 | `HttpOnly`, `Secure`, `SameSite=Lax`, `Path=/` |
| FE 접근 여부 | **불가능** (HttpOnly — JS 에서 `document.cookie` 로 못 읽음. 이게 의도된 보안 설계임) |
| API 호출 시 | `fetch(url, { credentials: 'include' })` / `axios.defaults.withCredentials = true` 만 설정하면 끝 |
| 쿠키 도메인 | `COOKIE_DOMAIN` env (예: `.burty.co.kr`) |

### 토큰 갱신 / 로그아웃

- **갱신**: BE 가 access 만료 시 401 → FE 가 `POST /auth/refresh` ({ refreshToken }) 호출.
  - ⚠️ 현재 refresh API 는 body 로 refresh token 을 받음. HttpOnly 쿠키라 JS 가 못 읽는데 그대로 body 로 요구하는 구조는 **모순**임 — BE 에 쿠키 기반 refresh 로 변경 요청을 같이 넣는 것을 권장. (별도 이슈로 트래킹)
- **로그아웃**: `POST /auth/logout` 호출 → BE 가 쿠키 즉시 만료 + 토큰 blacklist 처리.

---

## 2. URL 파라미터 분기 방식 — 확인 요청 사항

**맞음.** `OAUTH_SUCCESS_REDIRECT` (기본값 `/auth/callback`) 한 경로로 항상 redirect 되고, 쿼리 파라미터로 상태를 구분함.

### 성공 케이스

```
/auth/callback?newUser=true&profileComplete=false
/auth/callback?newUser=false&profileComplete=true
```

| 파라미터 | 의미 | 권장 동작 |
|---|---|---|
| `newUser=true` | 이번 콜백에서 신규 가입된 유저 | 온보딩 진입 |
| `newUser=false` | 기존 회원 재로그인 | 메인 진입 |
| `profileComplete=true` | 프로필 필수 항목 모두 채워짐 | 메인 진입 |
| `profileComplete=false` | 프로필 미완료 | 프로필 보완 페이지 진입 |

> 두 값은 항상 같이 옴. 보통 `(newUser=true, profileComplete=false)` → 온보딩, `(false, true)` → 메인.

### 실패 케이스

```
/auth/callback?error=<코드>
```

`error` 가 있으면 `newUser`/`profileComplete` 는 안 옴.

| error 코드 | 발생 상황 | UX 권장 |
|---|---|---|
| `user_cancelled` | 유저가 OAuth 동의 화면에서 취소 | 조용히 로그인 화면으로 (에러 토스트 X) |
| `missing_code` | provider 가 code 없이 callback 호출 | 로그인 화면 + "다시 시도해주세요" |
| `invalid_request` | OAuth 요청 자체가 잘못됨 | 로그인 화면 + 일반 에러 |
| `state_expired` | state 토큰 만료/위조 (10분 초과 등) | 로그인 화면 + "세션이 만료되었습니다" |
| `invalid_code` | code 가 이미 사용됐거나 만료 | 로그인 화면 + "다시 시도해주세요" |
| `unsupported_provider` | 지원하지 않는 provider | 로그인 화면 + 일반 에러 |
| `provider_error` | provider API 호출 실패 | 로그인 화면 + "잠시 후 다시" |
| `provider_unavailable` | provider 일시 장애 (server_error 등) | 로그인 화면 + "잠시 후 다시" |
| `invalid_token` | provider 가 발급한 토큰이 무효 | 로그인 화면 + 일반 에러 |
| `forbidden` | 권한 거부 | 로그인 화면 + 일반 에러 |
| `internal_error` | BE 내부 에러 | 로그인 화면 + "잠시 후 다시" |

> 위 코드는 `SocialAuthSupport.mapProviderError` / `mapBusinessError` 에서 확정 — 새 코드를 임의로 추가하지 말고 필요하면 BE 에 요청 바람.

### FE 처리 의사코드

```ts
// /auth/callback 페이지 진입 시
const params = new URLSearchParams(window.location.search);
const error = params.get('error');
const newUser = params.get('newUser') === 'true';
const profileComplete = params.get('profileComplete') === 'true';

if (error) {
  // 3번 항목 참고
  router.replace(`/login?reason=${error}`);
  return;
}

if (newUser || !profileComplete) {
  router.replace('/onboarding');
} else {
  router.replace('/');
}
```

---

## 3. 에러 시 redirect 위치 제안 — 홈 vs 로그인 화면

### 현재 BE 동작

BE 는 성공/실패 모두 동일한 한 경로(`OAUTH_SUCCESS_REDIRECT` = `/auth/callback`) 로 302 한다. 즉 **BE 는 "홈으로 보낸다"가 아니라 "콜백 페이지로 보낸다"** 이고, 거기서부터 어디로 갈지는 FE 몫이다.

> 커밋 메시지에서 "에러시 홈으로 이동"이라고 적힌 부분은 `safeRedirect` 의 최후 fallback 경로 (`frontendUrl + "/?error=..."`) 를 가리키는데, 이건 `OAUTH_SUCCESS_REDIRECT` URL 빌드 자체가 실패했을 때만 타는 비상 경로다. 일반적인 OAuth 에러는 전부 `/auth/callback?error=...` 로 옴.

### 의견: **로그인 화면으로 보내는 것이 맞다**

이유:
1. **사용자 의도와 일치** — 사용자는 "로그인하려고" 버튼을 눌렀는데 실패했으면 다시 시도할 수 있게 로그인 진입점에 떨궈주는 게 자연스럽다. 홈으로 보내면 "로그인 됐나? 안 됐나?" 가 모호해진다.
2. **세션 상태가 정의되지 않음** — 에러 케이스에선 쿠키가 set 되지 않으므로 비로그인 상태다. 비로그인 상태로 홈에 떨어지면 결국 보호 페이지에서 다시 로그인으로 튕길 수 있어 redirect 가 두 번 일어남.
3. **에러 메시지를 보여줄 자리가 명확** — 로그인 화면에 토스트/배너로 사유를 보여주는 UX 가 가장 정석.
4. **BE 변경 없이 FE 만으로 처리 가능** — `/auth/callback` 페이지에서 `error` 가 있으면 `replace('/login?reason=<error>')` 로 보내면 끝.

### 권장 안

```
정상  → /auth/callback?newUser=...&profileComplete=...  → FE 가 /onboarding 또는 /
실패  → /auth/callback?error=<code>                     → FE 가 /login?reason=<code>
```

특수 케이스:
- `user_cancelled` → 토스트 없이 `/login` 으로 (사용자가 직접 취소한 거라 에러 메시지 노출은 오히려 거슬림)
- `state_expired` / `invalid_code` → "세션이 만료되었습니다. 다시 로그인해주세요" 안내 후 `/login`
- `provider_unavailable` → "카카오/구글 일시 장애" 안내 후 `/login`
- `internal_error` 등 그 외 → "일시적인 오류가 발생했습니다" 안내 후 `/login`

BE 쪽 변경은 필요 없음. 다만 만약 FE 에서 `/login` 이 아니라 다른 경로(예: `/auth/error`)로 분리하고 싶다면 그건 그것대로 FE 라우팅으로 처리하면 됨.

---

## 4. 그 외 FE 가 알아두면 좋은 것

- **CORS**: `withCredentials` / `credentials: 'include'` 를 쓰는 순간 BE 의 `Access-Control-Allow-Origin` 은 `*` 가 아닌 명시적 origin 이어야 한다. 로컬 개발 origin (예: `http://localhost:3000`) 은 BE 에 등록 필요 — 막히면 BE 팀에 ping.
- **개발 환경에서 쿠키가 안 박힐 때**: `SameSite=Lax` + `Secure` 조합이므로 https 가 아니면 쿠키가 저장 안 됨. 로컬에서 테스트하려면 BE 의 `COOKIE_SECURE=false`, `COOKIE_SAMESITE=Lax` 로 띄운 환경에 붙어야 함.
- **state 파라미터**: `/authorize-url` 응답으로 받은 state 는 FE 가 따로 저장/검증할 필요 없음. BE 가 DB(`JpaOAuthStateStore`)에 보관하고 callback 에서 검증함.
- **redirectUri 커스터마이즈**: `/authorize-url?redirectUri=...` 로 호출 시 provider 콘솔에 사전 등록된 URI 만 사용 가능. 임의 URI 넣으면 provider 단에서 거부(카카오는 KOE006).

---

## 5. 정리

| 질문 | 답 |
|---|---|
| 토큰을 FE 가 받는가? | ❌ 받지 않음. BE 가 HttpOnly 쿠키로 직접 set. |
| FE 가 쿠키를 JS 로 읽어야 하는가? | ❌. `credentials: 'include'` 만 켜면 끝. |
| URL 파라미터에 토큰이 실리는가? | ❌. 오직 `error` / `newUser` / `profileComplete` 만. |
| 에러 시 redirect 는 어디로? | BE 는 `/auth/callback?error=...` 로 보냄. FE 에서 `/login` 으로 다시 보내는 것을 권장. |