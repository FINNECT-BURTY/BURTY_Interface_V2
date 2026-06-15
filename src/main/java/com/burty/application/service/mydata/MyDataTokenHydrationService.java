package com.burty.application.service.mydata;

import com.burty.adapter.out.store.TokenStore;
import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** DB에 저장된 토큰을 런타임 TokenStore로 복원. */
@Service
@RequiredArgsConstructor
public class MyDataTokenHydrationService {

  private static final String REFRESH_PREFIX = "refresh:";
  private static final String EXPIRES_PREFIX = "expires:";

  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final TokenStore tokenStore;

  public void hydrate(String userId, String institutionCode) {
    Optional<MyDataTokenBundle> bundle =
        linkedInstitutionPersistence.loadTokenBundle(userId, institutionCode);
    if (bundle.isEmpty()) {
      return;
    }
    String scopeKey = MyDataOAuthPort.scopeKey(userId, institutionCode);
    MyDataTokenBundle tokens = bundle.get();
    tokenStore.put(scopeKey, tokens.accessToken());
    if (tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
      tokenStore.put(REFRESH_PREFIX + scopeKey, tokens.refreshToken());
    }
    if (tokens.tokenExpiresAt() != null) {
      tokenStore.put(
          EXPIRES_PREFIX + scopeKey,
          String.valueOf(
              tokens
                  .tokenExpiresAt()
                  .atZone(java.time.ZoneId.systemDefault())
                  .toInstant()
                  .toEpochMilli()));
    }
  }

  public void hydrateOpenBanking(String userId) {
    linkedInstitutionPersistence
        .loadTokenBundle(userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)
        .ifPresent(
            bundle -> {
              tokenStore.put(OPEN_BANKING_ACCESS_PREFIX + userId, bundle.accessToken());
              if (bundle.refreshToken() != null && !bundle.refreshToken().isBlank()) {
                tokenStore.put(OPEN_BANKING_REFRESH_PREFIX + userId, bundle.refreshToken());
              }
            });
  }

  public void clearOpenBankingRuntimeTokens(String userId) {
    tokenStore.remove(OPEN_BANKING_ACCESS_PREFIX + userId);
    tokenStore.remove(OPEN_BANKING_REFRESH_PREFIX + userId);
  }

  public void clearRuntimeTokens(String userId, String institutionCode) {
    String scopeKey = MyDataOAuthPort.scopeKey(userId, institutionCode);
    tokenStore.remove(scopeKey);
    tokenStore.remove(REFRESH_PREFIX + scopeKey);
    tokenStore.remove(EXPIRES_PREFIX + scopeKey);
  }

  private static final String OPEN_BANKING_ACCESS_PREFIX = "openbanking:access:";
  private static final String OPEN_BANKING_REFRESH_PREFIX = "openbanking:refresh:";
}
