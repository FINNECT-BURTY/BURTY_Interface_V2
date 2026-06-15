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
    String state = UUID.randomUUID().toString();
    String payload = userId + "|" + institutionCode;
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
    int sep = payload.indexOf('|');
    if (sep < 0) {
      return new FinanceOAuthContext(payload, "MYDATA");
    }
    return new FinanceOAuthContext(payload.substring(0, sep), payload.substring(sep + 1));
  }

  public record FinanceOAuthContext(String userId, String institutionCode) {}
}
