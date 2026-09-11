package com.burty.application.service.mydata;

import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연동 한 건의 토큰을 무효화한다. 전송요구 철회·동의 만료·탈퇴·연동 해제가 모두 여기를 지난다.
 *
 * <p>토큰은 세 군데에 있다 — DB(원본), 런타임 저장소(사본), 정보제공자. 예전에는 경로마다 지우는 곳이 달랐다. 철회는 DB 만, 해제는 DB 와 런타임 저장소만
 * 지웠고, 정보제공자에는 아무 경로도 알리지 않았다. 런타임 저장소가 남은 탓에 철회 후에도 수집이 계속됐다(#142).
 *
 * <p>순서가 중요하다.
 *
 * <ol>
 *   <li>정보제공자에 보낼 토큰을 먼저 읽어 둔다. 로컬 무효화 뒤에는 읽을 수 없다.
 *   <li>로컬 무효화 — DB, 런타임 저장소, 링크 상태. 수집을 멈추는 것은 이것이고 반드시 끝나야 한다.
 *   <li>정보제공자 폐기 요청. 실패해도 로컬 무효화는 되돌리지 않는다 — 기관이 응답하지 않는다고 수집을 재개할 수는 없다.
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class MyDataGrantRevoker {

  public static final String LINK_REVOKED = "REVOKED";
  public static final String LINK_UNLINKED = "UNLINKED";

  private static final Logger log = LoggerFactory.getLogger(MyDataGrantRevoker.class);
  private static final int SUMMARY_MAX = 1000;

  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final MyDataLinkStatusRepository linkStatusRepository;
  private final MyDataOAuthPort myDataOAuthPort;
  private final MyDataTransmissionLogService transmissionLogService;

  /**
   * @param linkStatus 링크 상태에 남길 값. 철회·만료·탈퇴는 {@link #LINK_REVOKED}, 사용자 해제는 {@link #LINK_UNLINKED}.
   * @return 정보제공자 폐기까지 확인했으면 true
   */
  @Transactional
  public boolean revoke(String userId, String institutionCode, String linkStatus) {
    boolean openBanking =
        LinkedInstitutionPersistenceService.OPEN_BANKING_CODE.equals(institutionCode);
    String token = openBanking ? null : currentToken(userId, institutionCode);

    linkedInstitutionPersistence.markRevoked(userId, institutionCode);
    if (openBanking) {
      tokenHydrationService.clearOpenBankingRuntimeTokens(userId);
    } else {
      tokenHydrationService.clearRuntimeTokens(userId, institutionCode);
    }
    markLinkStatus(userId, institutionCode, linkStatus);

    if (openBanking) {
      // 오픈뱅킹 토큰은 오픈뱅킹 쪽 폐기 API 로 무효화해야 한다. 마이데이터 정보제공자에 보내면
      // 엉뚱한 기관에 토큰을 넘기는 셈이라 여기서는 로컬 무효화만 한다.
      return false;
    }
    if (token == null) {
      transmissionLogService.logOutbound(
          userId, institutionCode, "PROVIDER_REVOKE_SKIPPED", "no token to revoke");
      return false;
    }
    try {
      myDataOAuthPort.revokeGrant(institutionCode, token);
      transmissionLogService.logOutbound(userId, institutionCode, "PROVIDER_REVOKE", "success");
      return true;
    } catch (RuntimeException e) {
      // 로컬에서는 이미 멈췄으므로 수집은 없다. 다만 기관 쪽 토큰이 살아 있으니, 다시 요청할
      // 근거로 실패를 남긴다.
      log.warn(
          "정보제공자 토큰 폐기 실패 userId={} institution={} err={}", userId, institutionCode, e.toString());
      transmissionLogService.logOutbound(
          userId,
          institutionCode,
          "PROVIDER_REVOKE_FAILED",
          truncate(e.getClass().getSimpleName() + ": " + e.getMessage()));
      return false;
    }
  }

  /** 정보제공자에 보낼 토큰. 리프레시 토큰을 우선한다 — 그것을 폐기해야 재발급까지 막힌다. */
  private String currentToken(String userId, String institutionCode) {
    String scopeKey = MyDataOAuthPort.scopeKey(userId, institutionCode);
    String refresh = myDataOAuthPort.findRefreshToken(scopeKey);
    if (notBlank(refresh)) {
      return refresh;
    }
    String access = myDataOAuthPort.findAccessToken(scopeKey);
    if (notBlank(access)) {
      return access;
    }
    return linkedInstitutionPersistence
        .loadTokenBundle(userId, institutionCode)
        .map(MyDataGrantRevoker::preferredToken)
        .orElse(null);
  }

  private static String preferredToken(MyDataTokenBundle bundle) {
    return notBlank(bundle.refreshToken()) ? bundle.refreshToken() : bundle.accessToken();
  }

  private void markLinkStatus(String userId, String institutionCode, String linkStatus) {
    linkStatusRepository
        .findByUserIdAndInstitutionCode(userId, institutionCode)
        .ifPresent(
            entity -> {
              // 갱신 배치는 이 테이블의 ACTIVE 를 보고 돈다. 그대로 두면 배치가 철회한 연동을 골라 되살린다.
              entity.setStatus(linkStatus);
              if (LINK_UNLINKED.equals(linkStatus)) {
                entity.setUnlinkedAt(LocalDateTime.now());
              }
              linkStatusRepository.save(entity);
            });
  }

  private static boolean notBlank(String value) {
    return value != null && !value.isBlank();
  }

  private static String truncate(String value) {
    return value.length() <= SUMMARY_MAX ? value : value.substring(0, SUMMARY_MAX);
  }
}
