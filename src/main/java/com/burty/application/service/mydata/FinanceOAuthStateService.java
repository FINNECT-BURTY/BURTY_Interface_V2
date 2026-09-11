package com.burty.application.service.mydata;

import com.burty.adapter.out.store.ChallengeStore;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 마이데이터·오픈뱅킹 OAuth state(CSRF 방지) 관리. */
@Service
@RequiredArgsConstructor
public class FinanceOAuthStateService {

  public static final String PROVIDER_MYDATA = "mydata";
  public static final String PROVIDER_OPENBANKING = "openbanking";

  private static final String KEY_PREFIX = "finance-oauth:";
  private static final long TTL_SECONDS = 600L;

  private final ChallengeStore challengeStore;

  public String issue(String provider, String userId, String institutionCode) {
    return issue(provider, userId, institutionCode, null);
  }

  /**
   * @param frontendOrigin 인가를 마친 뒤 돌려보낼 FE. 콜백은 정보제공자가 브라우저를 보내 오는 것이라 그때는 FE 가 누구인지 알 수 없어서, 인가 요청
   *     때 묶어 둔다. 이미 허용 목록으로 검증한 값만 넘긴다.
   */
  public String issue(
      String provider, String userId, String institutionCode, String frontendOrigin) {
    String state = UUID.randomUUID().toString();
    String payload =
        userId + "|" + institutionCode + "|" + (frontendOrigin == null ? "" : frontendOrigin);
    challengeStore.put(KEY_PREFIX + provider + ":" + state, payload, TTL_SECONDS);
    return state;
  }

  public FinanceOAuthContext consume(String provider, String state) {
    if (state == null || state.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "OAuth state가 필요합니다.");
    }
    String key = KEY_PREFIX + provider + ":" + state.trim();
    String payload = challengeStore.get(key);
    if (payload == null || payload.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않거나 만료된 OAuth state입니다.");
    }
    challengeStore.remove(key);
    String[] parts = payload.split("\\|", -1);
    if (parts.length == 1) {
      return new FinanceOAuthContext(parts[0], "MYDATA", null);
    }
    String origin = parts.length > 2 && !parts[2].isBlank() ? parts[2] : null;
    return new FinanceOAuthContext(parts[0], parts[1], origin);
  }

  public record FinanceOAuthContext(String userId, String institutionCode, String frontendOrigin) {}
}
