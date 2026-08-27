package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.security.BiometricAuthPort;
import com.burty.application.service.family.TransferApprovalService;
import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.application.service.finance.TransferOrderWriter;
import com.burty.application.service.finance.TransferService;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.TransferPolicyProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.core.exception.ExternalCallUnresolvedException;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.model.TransferResult;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * 이체 오케스트레이션 단위 테스트.
 *
 * <p>중점은 <b>실패 경로</b>다. 정상 경로는 원래도 동작했다. 문제는 은행 응답을 못 받았을 때, 생체인증이 실패했을 때, 같은 멱등키가 다시 들어왔을 때 상태와
 * 한도가 어떻게 되느냐였고, 그게 돈이 새는 지점이었다.
 */
class TransferServiceIdempotencyTests {

  private static final String USER_ID = "1";
  private static final Clock FIXED =
      Clock.fixed(Instant.parse("2026-08-27T12:00:00Z"), ZoneId.of("Asia/Seoul"));

  private TransferService transferService;
  private TransferOrderWriter orderWriter;
  private TransferLimitGuard limitGuard;
  private OpenBankingPort openBankingPort;
  private BiometricAuthPort biometricAuthPort;
  private TransferApprovalService transferApprovalService;
  private TransferOrderRepository transferOrderRepository;

  @BeforeEach
  void setUp() {
    orderWriter = Mockito.mock(TransferOrderWriter.class);
    limitGuard = Mockito.mock(TransferLimitGuard.class);
    openBankingPort = Mockito.mock(OpenBankingPort.class);
    biometricAuthPort = Mockito.mock(BiometricAuthPort.class);

    AuditLogger auditLogger = Mockito.mock(AuditLogger.class);
    transferOrderRepository = Mockito.mock(TransferOrderRepository.class);
    UserSettingRepository userSettingRepository = Mockito.mock(UserSettingRepository.class);
    LinkedInstitutionPersistenceService linkedInstitutionPersistence =
        Mockito.mock(LinkedInstitutionPersistenceService.class);
    MyDataTokenHydrationService tokenHydrationService =
        Mockito.mock(MyDataTokenHydrationService.class);
    transferApprovalService = Mockito.mock(TransferApprovalService.class);
    // 기본은 보호자 승인 불필요. 승인 분기는 전용 테스트에서 따로 검증한다.
    Mockito.when(transferApprovalService.requiredGuardian(anyString(), anyLong()))
        .thenReturn(Optional.empty());

    Mockito.when(
            linkedInstitutionPersistence.loadTokenBundle(
                eq(USER_ID), eq(LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)))
        .thenReturn(
            Optional.of(
                new MyDataTokenBundle("access", "refresh", LocalDateTime.now().plusHours(1))));
    Mockito.when(biometricAuthPort.verifyAssertion(anyString(), any())).thenReturn(true);

    transferService =
        new TransferService(
            biometricAuthPort,
            auditLogger,
            transferOrderRepository,
            userSettingRepository,
            limitGuard,
            orderWriter,
            openBankingPort,
            linkedInstitutionPersistence,
            tokenHydrationService,
            transferApprovalService,
            new TransferPolicyProperties(),
            FIXED);
  }

  // ── 멱등성 ────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("같은 멱등키의 재요청은 은행을 다시 호출하지 않고 기존 결과를 반환한다")
  void replayOfCompletedTransferDoesNotCallBankAgain() {
    givenClaimReturnsExisting(order(TransferOrderEntity.Status.EXECUTED, "ob-tx-1"));

    TransferResult result =
        transferService.transfer(USER_ID, "111", "222", 1000L, "test", "assertion", "key-1");

    assertEquals("ob-tx-1", result.transferId());
    assertEquals("COMPLETED", result.status());
    Mockito.verify(openBankingPort, Mockito.never())
        .transfer(anyString(), anyString(), anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("아직 처리 중인 멱등키에 성공 응답을 주지 않는다 — 클라이언트가 완료로 오인하면 안 된다")
  void replayOfInProgressTransferReportsInProgress() {
    givenClaimReturnsExisting(order(TransferOrderEntity.Status.EXECUTING, null));

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () ->
                transferService.transfer(
                    USER_ID, "111", "222", 1000L, "test", "assertion", "key-1"));

    assertEquals(ErrorCode.TRANSFER_IN_PROGRESS, e.getErrorCode());
    Mockito.verify(openBankingPort, Mockito.never())
        .transfer(anyString(), anyString(), anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("결과 불명 상태의 멱등키 재요청은 재실행하지 않고 확인 중임을 알린다")
  void replayOfUnknownTransferReportsUnknown() {
    givenClaimReturnsExisting(order(TransferOrderEntity.Status.UNKNOWN, null));

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () ->
                transferService.transfer(
                    USER_ID, "111", "222", 1000L, "test", "assertion", "key-1"));

    assertEquals(ErrorCode.TRANSFER_RESULT_UNKNOWN, e.getErrorCode());
  }

  // ── 정상 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("신규 이체는 한도를 예약하고 은행 호출 후 EXECUTED 로 확정한다")
  void freshTransferReservesLimitAndExecutes() {
    givenFreshClaim(100L);
    Mockito.when(openBankingPort.transfer(eq(USER_ID), anyString(), anyString(), anyLong(), any()))
        .thenReturn(Map.of("transactionId", "ob-tx-9"));

    TransferResult result =
        transferService.transfer(USER_ID, "111", "222", 1000L, "test", "assertion", "key-2");

    assertEquals("COMPLETED", result.status());
    assertEquals("ob-tx-9", result.transferId());
    Mockito.verify(limitGuard).reserve(USER_ID, 1000L);
    Mockito.verify(orderWriter).markExecuted(100L, "ob-tx-9");
    Mockito.verify(limitGuard, Mockito.never()).release(anyString(), anyLong(), any());
  }

  // ── 실패 경로 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("은행 응답을 확인하지 못하면 UNKNOWN 으로 남기고 한도를 되돌리지 않는다 (출금됐을 수 있으므로)")
  void unresolvedBankCallMarksUnknownAndKeepsLimitReserved() {
    givenFreshClaim(100L);
    Mockito.when(openBankingPort.transfer(eq(USER_ID), anyString(), anyString(), anyLong(), any()))
        .thenThrow(new ExternalCallUnresolvedException("transfer", "read timeout", null));

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () ->
                transferService.transfer(
                    USER_ID, "111", "222", 1000L, "test", "assertion", "key-3"));

    assertEquals(ErrorCode.TRANSFER_RESULT_UNKNOWN, e.getErrorCode());
    Mockito.verify(orderWriter).markUnknown(eq(100L), anyString(), any(LocalDateTime.class));
    Mockito.verify(orderWriter, Mockito.never()).markFailed(anyLong(), anyString());
    Mockito.verify(limitGuard, Mockito.never()).release(anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("은행이 명확히 거절하면 FAILED 로 확정하고 한도를 되돌린다")
  void rejectedBankCallMarksFailedAndReleasesLimit() {
    givenFreshClaim(100L);
    Mockito.when(openBankingPort.transfer(eq(USER_ID), anyString(), anyString(), anyLong(), any()))
        .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "잔액 부족"));

    assertThrows(
        BusinessException.class,
        () -> transferService.transfer(USER_ID, "111", "222", 1000L, "t", "assertion", "key-4"));

    Mockito.verify(orderWriter).markFailed(eq(100L), anyString());
    Mockito.verify(limitGuard).release(USER_ID, 1000L, LocalDate.now(FIXED));
  }

  @Test
  @DisplayName("생체인증 실패 시 실패 기록이 남고 한도가 해제된다 — 예전에는 롤백되어 기록 자체가 사라졌다")
  void biometricFailurePersistsFailureAndReleasesLimit() {
    givenFreshClaim(100L);
    Mockito.when(biometricAuthPort.verifyAssertion(anyString(), any())).thenReturn(false);

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () -> transferService.transfer(USER_ID, "111", "222", 1000L, "t", "bad", "key-5"));

    assertEquals(ErrorCode.FORBIDDEN, e.getErrorCode());
    Mockito.verify(orderWriter).markFailed(eq(100L), anyString());
    Mockito.verify(limitGuard).release(USER_ID, 1000L, LocalDate.now(FIXED));
    Mockito.verify(openBankingPort, Mockito.never())
        .transfer(anyString(), anyString(), anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("한도 초과 시 주문은 FAILED 로 남고 은행 호출은 일어나지 않는다")
  void limitExceededMarksOrderFailed() {
    givenFreshClaim(100L);
    Mockito.doThrow(new BusinessException(ErrorCode.TRANSFER_LIMIT_EXCEEDED, "한도 초과"))
        .when(limitGuard)
        .reserve(anyString(), anyLong());

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () ->
                transferService.transfer(
                    USER_ID, "111", "222", 9_000_000L, "t", "assertion", "key-6"));

    assertEquals(ErrorCode.TRANSFER_LIMIT_EXCEEDED, e.getErrorCode());
    Mockito.verify(orderWriter).markFailed(eq(100L), any());
    Mockito.verify(openBankingPort, Mockito.never())
        .transfer(anyString(), anyString(), anyString(), anyLong(), any());
  }

  // ── 보호자 사전 승인 ────────────────────────────────────────────────────────

  @Test
  @DisplayName("승인 권한 보호자가 있으면 이체를 실행하지 않고 승인 대기 상태로 둔다")
  void guardianApprovalHoldsTransferInsteadOfExecuting() {
    givenFreshClaim(100L);
    Mockito.when(transferApprovalService.requiredGuardian(anyString(), anyLong()))
        .thenReturn(Optional.of("77"));

    BusinessException e =
        assertThrows(
            BusinessException.class,
            () ->
                transferService.transfer(
                    USER_ID, "111", "222", 2_000_000L, "t", "assertion", "key-8"));

    assertEquals(ErrorCode.TRANSFER_APPROVAL_REQUIRED, e.getErrorCode());
    Mockito.verify(orderWriter).markStatus(100L, TransferOrderEntity.Status.AWAITING_APPROVAL);
    Mockito.verify(transferApprovalService)
        .requestApproval(eq(100L), eq(USER_ID), eq("77"), eq(2_000_000L), anyString(), any());
    // 핵심: 승인 전에는 은행 호출도, 한도 예약도 일어나지 않는다.
    Mockito.verify(openBankingPort, Mockito.never())
        .transfer(anyString(), anyString(), anyString(), anyLong(), any());
    Mockito.verify(limitGuard, Mockito.never()).reserve(anyString(), anyLong());
  }

  // ── 취소 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("실행 전 이체는 취소되고 예약된 한도가 해제된다")
  void cancellableTransferIsCancelledAndLimitReleased() {
    TransferOrderEntity order = order(TransferOrderEntity.Status.AUTHORIZED, null);
    order.setRequestedAt(LocalDateTime.now(FIXED));
    Mockito.when(transferOrderRepository.findByUser_UserIdAndIdempotencyKey(1L, "key-c"))
        .thenReturn(Optional.of(order));

    transferService.cancelTransfer(USER_ID, "key-c", "사용자 요청");

    Mockito.verify(orderWriter).markCancelled(100L, "사용자 요청");
    Mockito.verify(limitGuard).release(USER_ID, 1000L, LocalDate.now(FIXED));
  }

  @Test
  @DisplayName("승인 대기 단계에서 취소하면 한도 해제는 일어나지 않는다 (예약 전이므로)")
  void cancellingBeforeReservationDoesNotReleaseLimit() {
    TransferOrderEntity order = order(TransferOrderEntity.Status.AWAITING_APPROVAL, null);
    order.setRequestedAt(LocalDateTime.now(FIXED));
    Mockito.when(transferOrderRepository.findByUser_UserIdAndIdempotencyKey(1L, "key-d"))
        .thenReturn(Optional.of(order));

    transferService.cancelTransfer(USER_ID, "key-d", "보호자 거절");

    Mockito.verify(orderWriter).markCancelled(100L, "보호자 거절");
    Mockito.verify(limitGuard, Mockito.never()).release(anyString(), anyLong(), any());
  }

  @Test
  @DisplayName("존재하지 않는 멱등키 취소는 TRANSFER_NOT_FOUND")
  void cancellingUnknownTransferFails() {
    Mockito.when(transferOrderRepository.findByUser_UserIdAndIdempotencyKey(1L, "nope"))
        .thenReturn(Optional.empty());

    BusinessException e =
        assertThrows(
            BusinessException.class, () -> transferService.cancelTransfer(USER_ID, "nope", "x"));
    assertEquals(ErrorCode.TRANSFER_NOT_FOUND, e.getErrorCode());
  }

  // ── 승인 후 실행 ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("보호자 승인 후 실행 경로는 멱등키를 다시 선점하지 않고 바로 은행을 호출한다")
  void executeApprovedSkipsClaimAndExecutes() {
    Mockito.when(openBankingPort.transfer(eq(USER_ID), anyString(), anyString(), anyLong(), any()))
        .thenReturn(Map.of("transactionId", "ob-approved"));

    TransferResult result =
        transferService.executeApproved(
            100L, USER_ID, "111", "222", 2_000_000L, "t", "assertion", "key-9");

    assertEquals("COMPLETED", result.status());
    assertEquals("ob-approved", result.transferId());
    Mockito.verify(orderWriter, Mockito.never())
        .claim(anyString(), anyString(), anyLong(), any(), anyString());
    Mockito.verify(limitGuard).reserve(USER_ID, 2_000_000L);
    Mockito.verify(orderWriter).markExecuted(100L, "ob-approved");
  }

  // ── 조회 ─────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("남의 이체는 ID 를 알아도 조회되지 않는다")
  void transferDetailIsScopedToOwner() {
    TransferOrderEntity othersOrder = order(TransferOrderEntity.Status.EXECUTED, "ob-x");
    othersOrder.getUser().setUserId(999L); // 다른 사용자 소유
    Mockito.when(transferOrderRepository.findByBankTransactionId("ob-x"))
        .thenReturn(Optional.of(othersOrder));

    assertNull(transferService.getTransfer(USER_ID, "ob-x"));
  }

  // ── 입력 검증 ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("금액이 0 이하이면 주문을 만들지 않는다")
  void nonPositiveAmountIsRejectedBeforeClaim() {
    assertThrows(
        BusinessException.class,
        () -> transferService.transfer(USER_ID, "111", "222", 0L, "t", "assertion", "key-7"));
    Mockito.verify(orderWriter, Mockito.never())
        .claim(anyString(), anyString(), anyLong(), any(), anyString());
  }

  // ── 헬퍼 ─────────────────────────────────────────────────────────────────

  private void givenFreshClaim(long orderId) {
    TransferOrderEntity created = order(TransferOrderEntity.Status.PENDING, null);
    created.setOrderId(orderId);
    Mockito.when(orderWriter.claim(anyString(), anyString(), anyLong(), any(), anyString()))
        .thenReturn(new TransferOrderWriter.Claim(created, true));
  }

  private void givenClaimReturnsExisting(TransferOrderEntity existing) {
    Mockito.when(orderWriter.claim(anyString(), anyString(), anyLong(), any(), anyString()))
        .thenReturn(new TransferOrderWriter.Claim(existing, false));
  }

  private static TransferOrderEntity order(
      TransferOrderEntity.Status status, String bankTransactionId) {
    TransferOrderEntity entity = new TransferOrderEntity();
    UserEntity user = new UserEntity();
    user.setUserId(1L);
    entity.setUser(user);
    entity.setOrderId(100L);
    entity.setAmount(1000L);
    entity.setStatus(status);
    entity.setBankTransactionId(bankTransactionId);
    entity.setToAccountNo("222");
    entity.setToAccountNoMasked("2**");
    return entity;
  }
}
