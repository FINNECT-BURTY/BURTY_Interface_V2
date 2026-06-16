/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (AccountNumberHasher)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 계좌번호·민감 문자열의 SHA-256 해시(인덱스 조회용). 원문 PII는 저장하지 않고, 화면 표시는 {@link #mask}로 마스킹합니다. */
@Component
public class AccountNumberHasher {

  public String hash(String accountNo) {
    if (accountNo == null) return null;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(accountNo.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public String mask(String accountNo) {
    if (accountNo == null || accountNo.length() < 4) return "****";
    int visibleTail = Math.min(4, accountNo.length() - 4);
    int hidden = accountNo.length() - visibleTail;
    return "*".repeat(Math.max(4, hidden)) + accountNo.substring(accountNo.length() - visibleTail);
  }
}
