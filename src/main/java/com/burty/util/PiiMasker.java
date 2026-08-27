package com.burty.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그·감사 기록에 남길 값에서 개인정보를 가린다.
 *
 * <p>두 가지 방식으로 쓴다.
 *
 * <ol>
 *   <li><b>호출부에서 명시적으로</b> — {@code log.info("... {}", PiiMasker.account(no))}. 의도가 코드에 드러나므로 이쪽이
 *       원칙이다.
 *   <li><b>패턴 기반 사후 스크럽</b> — {@link #scrub(String)}. 예외 메시지처럼 우리가 문장을 통제할 수 없는 경로를 위한 안전망이다. DB 제약
 *       위반 메시지에는 위반한 컬럼 값이 그대로 들어간다.
 * </ol>
 *
 * <p>1번만으로는 부족하다. 로그 문장은 계속 늘어나고 그때마다 마스킹을 기억할 수 없다. 2번만으로도 부족하다. 패턴에 걸리지 않는 형태는 놓친다. 그래서 둘 다 둔다.
 */
public final class PiiMasker {

  private static final String REDACTED = "***";

  /** 계좌번호 — 숫자/하이픈 10~20자리. 너무 짧으면 금액·날짜와 구분되지 않으므로 10자리 이상만 본다. */
  private static final Pattern ACCOUNT_LIKE =
      Pattern.compile(
          "(?<![0-9])(?:[0-9]{2,6}-){1,3}[0-9]{2,8}(?![0-9])|(?<![0-9])[0-9]{10,20}(?![0-9])");

  /** 휴대전화 — 010-1234-5678, 01012345678, +82-10-... */
  private static final Pattern PHONE =
      Pattern.compile(
          "(?<![0-9])(?:\\+?82[-\\s]?)?01[0-9][-\\s]?[0-9]{3,4}[-\\s]?[0-9]{4}(?![0-9])");

  /** 주민등록번호 — 6자리-7자리. 절대 로그에 남으면 안 되는 값이다. */
  private static final Pattern RRN =
      Pattern.compile("(?<![0-9])[0-9]{6}[-\\s]?[1-4][0-9]{6}(?![0-9])");

  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

  /** JWT 및 유사 베어러 토큰. */
  private static final Pattern JWT =
      Pattern.compile("eyJ[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{4,}");

  /** {@code accessToken=...}, {@code "password":"..."} 같은 키-값 형태의 비밀값. */
  private static final Pattern SECRET_KV =
      Pattern.compile(
          "(?i)\"?\\b(password|passwd|secret|token|accessToken|refreshToken|apiKey|api_key"
              + "|authorization|ci|ciHash|assertionToken)\\b\"?"
              + "\\s*[=:]\\s*\"?([^\\s,;}\"]+)\"?");

  private PiiMasker() {}

  // ── 명시적 마스킹 ──────────────────────────────────────────────────────────

  /** 계좌번호 — 뒤 4자리만 남긴다. */
  public static String account(String accountNo) {
    return keepTail(accountNo, 4);
  }

  /** 전화번호 — 뒤 4자리만 남긴다. */
  public static String phone(String phone) {
    return keepTail(phone, 4);
  }

  /** 이름 — 첫 글자만 남긴다. (홍길동 → 홍**) */
  public static String name(String name) {
    if (name == null || name.isBlank()) {
      return name;
    }
    return name.charAt(0) + REDACTED;
  }

  /** 이메일 — 로컬 파트 앞 2자만 남긴다. */
  public static String email(String email) {
    if (email == null || email.isBlank()) {
      return email;
    }
    int at = email.indexOf('@');
    if (at <= 0) {
      return REDACTED;
    }
    String local = email.substring(0, at);
    String head = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
    return head + REDACTED + email.substring(at);
  }

  /**
   * 토큰·비밀값 — 전부 가린다.
   *
   * <p>앞 몇 자를 남기면 "같은 토큰인지" 를 눈으로 비교할 수 있어 디버깅에 편하지만, 그만큼 유출 시 상관관계 분석이 가능해진다. 대신 길이만 남겨 형식 오류 정도는
   * 구분할 수 있게 한다.
   */
  public static String secret(String value) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return REDACTED + "(len=" + value.length() + ")";
  }

  /** 값의 뒷자리만 남기고 가린다. */
  private static String keepTail(String value, int tailLength) {
    if (value == null || value.isBlank()) {
      return value;
    }
    String digitsOnly = value.replaceAll("[^0-9]", "");
    if (digitsOnly.length() <= tailLength) {
      return REDACTED;
    }
    return REDACTED + digitsOnly.substring(digitsOnly.length() - tailLength);
  }

  // ── 패턴 기반 스크럽 (안전망) ────────────────────────────────────────────────

  /**
   * 임의의 문자열에서 개인정보로 보이는 부분을 가린다.
   *
   * <p>우리가 문장을 통제할 수 없는 경로(예외 메시지, 외부 API 응답 본문 등)를 위한 것이다. 순서가 중요하다. 주민번호·전화번호를 먼저 처리하지 않으면 계좌번호
   * 패턴이 먼저 먹어버린다.
   */
  public static String scrub(String message) {
    if (message == null || message.isEmpty()) {
      return message;
    }
    String result = message;
    result = SECRET_KV.matcher(result).replaceAll(mr -> mr.group(1) + "=" + REDACTED);
    result = JWT.matcher(result).replaceAll(REDACTED);
    result = RRN.matcher(result).replaceAll(REDACTED);
    result = PHONE.matcher(result).replaceAll(mr -> maskKeepingTail(mr.group(), 4));
    result = EMAIL.matcher(result).replaceAll(mr -> email(mr.group()));
    result = ACCOUNT_LIKE.matcher(result).replaceAll(mr -> maskKeepingTail(mr.group(), 4));
    return result;
  }

  private static String maskKeepingTail(String matched, int tailLength) {
    String digits = matched.replaceAll("[^0-9]", "");
    if (digits.length() <= tailLength) {
      return REDACTED;
    }
    return Matcher.quoteReplacement(REDACTED + digits.substring(digits.length() - tailLength));
  }
}
