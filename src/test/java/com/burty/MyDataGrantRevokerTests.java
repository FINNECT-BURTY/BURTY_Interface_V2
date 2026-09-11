package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.application.port.out.mydata.MyDataOAuthPort;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataGrantRevoker;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.mydata.MyDataTransmissionLogService;
import com.burty.domain.mydata.entity.MyDataLinkStatusEntity;
import com.burty.domain.mydata.repository.MyDataLinkStatusRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/**
 * 토큰 무효화의 순서와 실패 처리를 못 박는다.
 *
 * <p>무효화는 세 군데(DB·런타임 저장소·정보제공자)에 걸쳐 있고, 순서가 틀리면 둘 중 하나가 깨진다. 로컬을 먼저 지우면 정보제공자에 보낼 토큰을 잃고, 정보제공자를
 * 먼저 기다리면 기관이 응답하지 않는 동안 수집이 계속된다.
 */
class MyDataGrantRevokerTests {

  private static final String USER_ID = "7";
  private static final String INSTITUTION = "KB";
  private static final String SCOPE_KEY = MyDataOAuthPort.scopeKey(USER_ID, INSTITUTION);

  private LinkedInstitutionPersistenceService persistence;
  private MyDataTokenHydrationService hydration;
  private MyDataLinkStatusRepository linkStatusRepository;
  private MyDataOAuthPort oauthPort;
  private MyDataTransmissionLogService transmissionLog;
  private MyDataGrantRevoker revoker;

  @BeforeEach
  void setUp() {
    persistence = mock(LinkedInstitutionPersistenceService.class);
    hydration = mock(MyDataTokenHydrationService.class);
    linkStatusRepository = mock(MyDataLinkStatusRepository.class);
    oauthPort = mock(MyDataOAuthPort.class);
    transmissionLog = mock(MyDataTransmissionLogService.class);
    when(persistence.loadTokenBundle(anyString(), anyString())).thenReturn(Optional.empty());
    revoker =
        new MyDataGrantRevoker(
            persistence, hydration, linkStatusRepository, oauthPort, transmissionLog);
  }

  @Test
  @DisplayName("로컬 무효화 전에 읽어 둔 리프레시 토큰으로, 로컬 무효화 뒤에 정보제공자 폐기를 요청한다")
  void revokesAtProviderAfterLocalInvalidationWithTokenReadBefore() {
    when(oauthPort.findRefreshToken(SCOPE_KEY)).thenReturn("rt-before");

    assertTrue(revoker.revoke(USER_ID, INSTITUTION, MyDataGrantRevoker.LINK_REVOKED));

    InOrder order = inOrder(oauthPort, persistence, hydration);
    order.verify(oauthPort).findRefreshToken(SCOPE_KEY);
    order.verify(persistence).markRevoked(USER_ID, INSTITUTION);
    order.verify(hydration).clearRuntimeTokens(USER_ID, INSTITUTION);
    order.verify(oauthPort).revokeGrant(INSTITUTION, "rt-before");
  }

  @Test
  @DisplayName("정보제공자 폐기가 실패해도 로컬 무효화는 끝나 있고 실패를 기록한다")
  void providerFailureDoesNotUndoLocalInvalidation() {
    // 기관이 응답하지 않는다고 수집을 재개할 수는 없다. 대신 다시 요청할 근거를 남긴다.
    when(oauthPort.findRefreshToken(SCOPE_KEY)).thenReturn("rt-before");
    doThrow(new IllegalStateException("provider timeout"))
        .when(oauthPort)
        .revokeGrant(INSTITUTION, "rt-before");

    assertFalse(revoker.revoke(USER_ID, INSTITUTION, MyDataGrantRevoker.LINK_REVOKED));

    verify(persistence).markRevoked(USER_ID, INSTITUTION);
    verify(hydration).clearRuntimeTokens(USER_ID, INSTITUTION);
    verify(transmissionLog)
        .logOutbound(eq(USER_ID), eq(INSTITUTION), eq("PROVIDER_REVOKE_FAILED"), anyString());
  }

  @Test
  @DisplayName("링크 상태를 내려 갱신 배치가 그 연동을 고르지 못하게 한다")
  void marksLinkStatusSoRefreshBatchSkipsIt() {
    MyDataLinkStatusEntity status = new MyDataLinkStatusEntity();
    status.setStatus("ACTIVE");
    when(linkStatusRepository.findByUserIdAndInstitutionCode(USER_ID, INSTITUTION))
        .thenReturn(Optional.of(status));

    revoker.revoke(USER_ID, INSTITUTION, MyDataGrantRevoker.LINK_REVOKED);

    assertEquals("REVOKED", status.getStatus());
    verify(linkStatusRepository).save(status);
  }

  @Test
  @DisplayName("사용자 해제는 UNLINKED 와 해제 시각을 남긴다")
  void unlinkRecordsUnlinkedAt() {
    MyDataLinkStatusEntity status = new MyDataLinkStatusEntity();
    status.setStatus("ACTIVE");
    when(linkStatusRepository.findByUserIdAndInstitutionCode(USER_ID, INSTITUTION))
        .thenReturn(Optional.of(status));

    revoker.revoke(USER_ID, INSTITUTION, MyDataGrantRevoker.LINK_UNLINKED);

    assertEquals("UNLINKED", status.getStatus());
    assertNotNull(status.getUnlinkedAt());
  }

  @Test
  @DisplayName("오픈뱅킹은 마이데이터 정보제공자에 폐기를 보내지 않고 오픈뱅킹 런타임 토큰을 지운다")
  void openBankingIsNotSentToMyDataProvider() {
    // 탈퇴는 오픈뱅킹 연동까지 같은 경로로 돌린다. 여기서 가르지 않으면 오픈뱅킹 토큰을
    // 엉뚱한 기관에 넘기고, 키가 달라 런타임 토큰도 지워지지 않는다.
    String code = LinkedInstitutionPersistenceService.OPEN_BANKING_CODE;

    assertFalse(revoker.revoke(USER_ID, code, MyDataGrantRevoker.LINK_REVOKED));

    verify(persistence).markRevoked(USER_ID, code);
    verify(hydration).clearOpenBankingRuntimeTokens(USER_ID);
    verify(hydration, never()).clearRuntimeTokens(anyString(), anyString());
    verify(oauthPort, never()).revokeGrant(anyString(), anyString());
  }

  @Test
  @DisplayName("보낼 토큰이 없으면 폐기 요청 없이 로컬만 무효화하고 그 사실을 남긴다")
  void noTokenSkipsProviderButStillInvalidatesLocally() {
    assertFalse(revoker.revoke(USER_ID, INSTITUTION, MyDataGrantRevoker.LINK_REVOKED));

    verify(persistence).markRevoked(USER_ID, INSTITUTION);
    verify(hydration).clearRuntimeTokens(USER_ID, INSTITUTION);
    verify(oauthPort, never()).revokeGrant(anyString(), anyString());
    verify(transmissionLog)
        .logOutbound(eq(USER_ID), eq(INSTITUTION), eq("PROVIDER_REVOKE_SKIPPED"), any());
  }
}
