package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.util.PiiMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 로그 개인정보 마스킹 검증.
 *
 * <p>핵심 판정 기준은 하나다. <b>원본 값이 결과 문자열에 남아 있으면 실패다.</b> 형식이 예뻐지는 것보다 값이 사라지는 것이 중요하다.
 */
class PiiMaskerTests {

  @Nested
  @DisplayName("명시적 마스킹")
  class Explicit {

    @Test
    @DisplayName("계좌번호는 뒤 4자리만 남는다")
    void accountKeepsOnlyTail() {
      assertEquals("***7890", PiiMasker.account("1234567890"));
      assertEquals("***7890", PiiMasker.account("110-234-567890"));
      assertEquals("***", PiiMasker.account("123"), "너무 짧으면 전부 가린다");
    }

    @Test
    @DisplayName("전화번호는 뒤 4자리만 남는다")
    void phoneKeepsOnlyTail() {
      assertEquals("***5678", PiiMasker.phone("010-1234-5678"));
      assertEquals("***5678", PiiMasker.phone("01012345678"));
    }

    @Test
    @DisplayName("이름은 첫 글자만 남는다")
    void nameKeepsFirstCharacter() {
      assertEquals("홍***", PiiMasker.name("홍길동"));
      assertEquals("K***", PiiMasker.name("Kim"));
    }

    @Test
    @DisplayName("이메일은 로컬 파트 앞 2자만 남는다")
    void emailKeepsPrefix() {
      assertEquals("ro***@example.com", PiiMasker.email("rosie@example.com"));
      assertTrue(PiiMasker.email("a@b.co").startsWith("a***"));
    }

    @Test
    @DisplayName("토큰은 전부 가리고 길이만 남긴다")
    void secretIsFullyRedacted() {
      String token = "eyJhbGciOiJIUzI1NiJ9.payload.signature";
      String masked = PiiMasker.secret(token);
      assertFalse(masked.contains("payload"), "토큰 조각이 남으면 상관관계 분석이 가능해진다");
      assertTrue(masked.contains("len=" + token.length()), "형식 오류 구분용으로 길이는 남긴다");
    }

    @Test
    @DisplayName("null 과 빈 값은 그대로 통과한다")
    void blankValuesPassThrough() {
      assertEquals(null, PiiMasker.account(null));
      assertEquals("", PiiMasker.phone(""));
      assertEquals(null, PiiMasker.secret(null));
    }
  }

  @Nested
  @DisplayName("패턴 기반 스크럽 — 통제 못 하는 문자열용 안전망")
  class Scrub {

    @Test
    @DisplayName("주민등록번호는 흔적도 남기지 않는다")
    void residentRegistrationNumberIsRemoved() {
      String scrubbed = PiiMasker.scrub("본인확인 실패 rrn=901231-1234567 code=E01");
      assertFalse(scrubbed.contains("901231"), "앞 6자리(생년월일)도 남으면 안 된다");
      assertFalse(scrubbed.contains("1234567"));
    }

    @Test
    @DisplayName("DB 제약 위반 메시지의 계좌번호를 가린다")
    void accountNumberInConstraintViolationIsMasked() {
      String raw =
          "Duplicate entry '110234567890' for key 'uk_registered_account' "
              + "; SQL [insert into tbl_registered_account (account_no) values (?)]";
      String scrubbed = PiiMasker.scrub(raw);
      assertFalse(scrubbed.contains("110234567890"), "예외 메시지는 우리가 통제할 수 없는 대표적 유출 경로다");
      assertTrue(scrubbed.contains("***7890"));
    }

    @Test
    @DisplayName("전화번호를 가린다")
    void phoneIsMasked() {
      String scrubbed = PiiMasker.scrub("사용자 조회 실패 phone=010-1234-5678");
      assertFalse(scrubbed.contains("010-1234-5678"));
      assertTrue(scrubbed.contains("***5678"));
    }

    @Test
    @DisplayName("JWT 를 가린다")
    void jwtIsMasked() {
      String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcdEFGH1234";
      String scrubbed = PiiMasker.scrub("Authorization 검증 실패: " + jwt);
      assertFalse(scrubbed.contains("eyJzdWIiOiIxIn0"));
    }

    @ParameterizedTest
    @DisplayName("키-값 형태의 비밀값을 가린다")
    @ValueSource(
        strings = {
          "password=hunter2",
          "accessToken: abc123def456",
          "\"refreshToken\":\"zzz999\"",
          "apiKey=sk-live-0123456789"
        })
    void secretKeyValuePairsAreMasked(String input) {
      String scrubbed = PiiMasker.scrub("요청 실패 " + input);
      assertFalse(
          scrubbed.matches(".*(hunter2|abc123def456|zzz999|sk-live-0123456789).*"),
          "비밀값이 남았습니다: " + scrubbed);
    }

    @Test
    @DisplayName("이메일을 가린다")
    void emailIsMasked() {
      String scrubbed = PiiMasker.scrub("메일 발송 실패 to=rosie@example.com");
      assertFalse(scrubbed.contains("rosie@example.com"));
    }

    @Test
    @DisplayName("금액·타임스탬프·ID 같은 정상 값은 건드리지 않는다")
    void operationalValuesSurvive() {
      String raw = "이체 완료 orderId=42 amount=1000000 elapsed=350ms status=EXECUTED";
      String scrubbed = PiiMasker.scrub(raw);
      assertTrue(scrubbed.contains("orderId=42"), "짧은 ID 는 유지되어야 운영 추적이 가능하다");
      assertTrue(scrubbed.contains("status=EXECUTED"));
      assertTrue(scrubbed.contains("elapsed=350ms"));
    }

    @Test
    @DisplayName("null 과 빈 문자열을 안전하게 처리한다")
    void handlesNullAndEmpty() {
      assertEquals(null, PiiMasker.scrub(null));
      assertEquals("", PiiMasker.scrub(""));
    }
  }
}
