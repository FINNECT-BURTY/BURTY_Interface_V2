/**
 *
 *
 * <pre>
 * <b>Description  : 금융 애플리케이션 서비스 (TransferService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.finance
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
package com.burty.application.service.finance;

import com.burty.application.port.in.finance.TransferUseCase;
import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.security.BiometricAuthPort;
import com.burty.application.service.family.TransferApprovalService;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.support.AuditLogger;
import com.burty.config.TransferPolicyProperties;
import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.core.exception.ExternalCallUnresolvedException;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.model.TransferResult;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이체 오케스트레이션.
 *
 * <p><b>이 클래스에는 {@code @Transactional} 이 없다.</b> 의도적이다. 은행 호출은 네트워크 왕복이고, 그것을 DB 트랜잭션 안에 넣으면 (1)
 * 커넥션을 왕복 시간만큼 점유하고 (2) 실패 시 롤백되어 실패 기록조차 남지 않으며 (3) 은행 호출 성공 후 커밋이 실패하면 돈은 나갔는데 기록이 없는 상태가 된다. 상태
 * 전이는 {@link TransferOrderWriter} 가 독립 트랜잭션으로 처리하고, 여기서는 순서만 조율한다.
 *
 * <p>실행 순서와 각 단계의 실패 처리:
 *
 * <pre>
 *   1. 입력 검증
 *   2. 멱등키 선점(INSERT)        → 중복이면 기존 결과 반환
 *   3. 한도 예약(원자적 UPDATE)    → 실패 시 주문 FAILED, 한도 미소모
 *   4. 생체인증 검증               → 실패 시 주문 FAILED, 한도 해제
 *   5. 은행 이체 호출 (트랜잭션 밖)
 *        성공     → EXECUTED
 *        명확한 거절 → FAILED  + 한도 해제
 *        응답 불명 → UNKNOWN + 한도 유지(출금됐을 수 있음) + 정산 배치로 이관
 * </pre>
 */
@Service
public class TransferService implements TransferUseCase {

  private static final Logger log = LoggerFactory.getLogger(TransferService.class);
  private static final String LIMIT_KEY = "TRANSFER_LIMIT";

  private final BiometricAuthPort biometricAuthPort;
  private final AuditLogger auditLogger;
  private final TransferOrderRepository transferOrderRepository;
  private final UserSettingRepository userSettingRepository;
  private final TransferLimitGuard transferLimitGuard;
  private final TransferOrderWriter orderWriter;
  private final OpenBankingPort openBankingPort;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final TransferApprovalService transferApprovalService;
  private final TransferPolicyProperties policy;
  private final Clock clock;

  public TransferService(
      BiometricAuthPort biometricAuthPort,
      AuditLogger auditLogger,
      TransferOrderRepository transferOrderRepository,
      UserSettingRepository userSettingRepository,
      TransferLimitGuard transferLimitGuard,
      TransferOrderWriter orderWriter,
      OpenBankingPort openBankingPort,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTokenHydrationService tokenHydrationService,
      TransferApprovalService transferApprovalService,
      TransferPolicyProperties policy,
      Clock clock) {
    this.biometricAuthPort = biometricAuthPort;
    this.auditLogger = auditLogger;
    this.transferOrderRepository = transferOrderRepository;
    this.userSettingRepository = userSettingRepository;
    this.transferLimitGuard = transferLimitGuard;
    this.orderWriter = orderWriter;
    this.openBankingPort = openBankingPort;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.tokenHydrationService = tokenHydrationService;
    this.transferApprovalService = transferApprovalService;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public TransferResult transfer(
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String idempotencyKey) {
    validateTransferInput(userId, fromAccount, toAccount, amount);
    requireOpenBankingLink(userId);

    String effectiveKey = normalizeKey(idempotencyKey);

    // --- 2. 멱등키 선점 ------------------------------------------------------
    TransferOrderWriter.Claim claim =
        orderWriter.claim(userId, toAccount, amount, description, effectiveKey);
    if (!claim.fresh()) {
      return replayResult(claim.order());
    }

    Long orderId = claim.order().getOrderId();

    // --- 2-1. 보호자 사전 승인 -------------------------------------------------
    // 기존 가족 보호는 이체가 끝난 뒤 알리기만 했다. 알림 시점에는 이미 돈이 나간 뒤라
    // 보이스피싱 피해를 막지 못한다. 임계 금액 이상이고 승인 권한을 가진 보호자가 있으면
    // 여기서 이체를 보류한다.
    var guardian = transferApprovalService.requiredGuardian(userId, amount);
    if (guardian.isPresent()) {
      orderWriter.markStatus(orderId, TransferOrderEntity.Status.AWAITING_APPROVAL);
      transferApprovalService.requestApproval(
          orderId, userId, guardian.get(), amount, maskAccount(toAccount), description);
      throw new BusinessException(
          ErrorCode.TRANSFER_APPROVAL_REQUIRED, AppMessages.Transfer.APPROVAL_REQUIRED);
    }

    return runTransfer(
        orderId, userId, fromAccount, toAccount, amount, description, assertionToken, effectiveKey);
  }

  /**
   * 한도 예약 → 생체인증 → 은행 호출 본체.
   *
   * <p>일반 이체와 보호자 승인 후 이체가 공유하는 경로다. 승인 후 경로는 이미 주문이 존재하므로 멱등키 선점 단계만 건너뛴다.
   */
  private TransferResult runTransfer(
      Long orderId,
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String effectiveKey) {

    // --- 3. 한도 예약 --------------------------------------------------------
    // 차감한 날짜를 주문에 기록해 둔다. 해제할 때 날짜를 다시 계산하면 자정을 걸친 이체에서
    // 다른 행을 가리킨다.
    LocalDate reservedDate;
    try {
      reservedDate = transferLimitGuard.reserve(userId, amount);
      orderWriter.markLimitReserved(orderId, reservedDate);
    } catch (RuntimeException e) {
      orderWriter.markFailed(orderId, e.getMessage());
      throw e;
    }

    // --- 4. 생체인증 ---------------------------------------------------------
    orderWriter.markStatus(orderId, TransferOrderEntity.Status.AUTH_REQUESTED);
    if (!biometricAuthPort.verifyAssertion(userId, assertionToken)) {
      orderWriter.markFailed(orderId, AppMessages.Transfer.WEBAUTHN_VERIFY_FAILED);
      transferLimitGuard.release(userId, amount, reservedDate);
      auditLogger.logFailure(
          userId, "TRANSFER", toAccount, AppMessages.Transfer.WEBAUTHN_VERIFY_FAILED);
      throw new BusinessException(ErrorCode.FORBIDDEN, AppMessages.Transfer.WEBAUTHN_VERIFY_FAILED);
    }
    orderWriter.markStatus(orderId, TransferOrderEntity.Status.AUTHORIZED);

    // --- 5. 은행 호출 (트랜잭션 밖) -------------------------------------------
    tokenHydrationService.hydrateOpenBanking(userId);
    orderWriter.markStatus(orderId, TransferOrderEntity.Status.EXECUTING);
    try {
      Map<String, Object> response =
          openBankingPort.transfer(userId, fromAccount, toAccount, amount, effectiveKey);
      String bankTxnId = extractTransactionId(response);
      orderWriter.markExecuted(orderId, bankTxnId);
      auditLogger.logSuccess(
          userId, "TRANSFER", toAccount, "amount=" + amount + ", description=" + description);
      return new TransferResult(bankTxnId, "COMPLETED", false);

    } catch (ExternalCallUnresolvedException e) {
      // 출금됐을 수도 있다. FAILED 로 확정하지 않고 한도도 되돌리지 않는다.
      LocalDateTime nextCheck =
          LocalDateTime.now(clock).plusSeconds(policy.getReconcileInitialDelaySeconds());
      orderWriter.markUnknown(orderId, e.getMessage(), nextCheck);
      auditLogger.logFailure(userId, "TRANSFER", toAccount, "결과 확인 불가: " + e.getMessage());
      log.error(
          "이체 결과 확인 불가 — 정산 대상 등록 orderId={} userId={} amount={} reason={}",
          orderId,
          userId,
          amount,
          e.getMessage(),
          e);
      throw new BusinessException(
          ErrorCode.TRANSFER_RESULT_UNKNOWN, AppMessages.Transfer.RESULT_UNKNOWN, e);

    } catch (RuntimeException e) {
      // 은행이 명확히 거절한 경우 — 출금이 없었으므로 한도를 되돌린다.
      orderWriter.markFailed(orderId, AppMessages.Transfer.ORDER_FAILED_PREFIX + e.getMessage());
      transferLimitGuard.release(userId, amount, reservedDate);
      auditLogger.logFailure(userId, "TRANSFER", toAccount, e.getMessage());
      throw e;
    }
  }

  /**
   * 보호자 승인이 완료된 이체를 실행한다.
   *
   * <p>승인 시점에 다시 호출되는 경로다. 이미 멱등키가 선점된 주문이므로 새로 claim 하지 않는다.
   */
  public TransferResult executeApproved(
      Long orderId,
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String idempotencyKey) {
    requireOpenBankingLink(userId);
    return runTransfer(
        orderId,
        userId,
        fromAccount,
        toAccount,
        amount,
        description,
        assertionToken,
        idempotencyKey);
  }

  /** 사용자가 아직 실행되지 않은 이체를 취소한다. */
  @Override
  public void cancelTransfer(String userId, String idempotencyKey, String reason) {
    long numericUserId = Long.parseLong(userId);
    TransferOrderEntity order =
        transferOrderRepository
            .findByUser_UserIdAndIdempotencyKey(numericUserId, idempotencyKey)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.TRANSFER_NOT_FOUND, AppMessages.Transfer.NOT_FOUND));

    orderWriter.markCancelled(order.getOrderId(), reason);
    // 예약이 잡혀 있었을 때만 되돌린다. limitUsageDate 가 null 이면 차감한 적이 없다.
    if (order.getLimitUsageDate() != null) {
      transferLimitGuard.release(userId, order.getAmount(), order.getLimitUsageDate());
    }
    auditLogger.logSuccess(
        userId, "TRANSFER_CANCELLED", String.valueOf(order.getOrderId()), reason);
  }

  private static String maskAccount(String accountNo) {
    if (accountNo == null || accountNo.length() <= 4) {
      return "****";
    }
    return "*".repeat(accountNo.length() - 4) + accountNo.substring(accountNo.length() - 4);
  }

  private void requireOpenBankingLink(String userId) {
    if (linkedInstitutionPersistence
        .loadTokenBundle(userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)
        .isEmpty()) {
      throw new BusinessException(
          ErrorCode.DATA_NOT_FOUND, AppMessages.Transfer.OPENBANKING_NOT_LINKED);
    }
  }

  /** 같은 멱등키의 재요청. 아직 진행 중이면 결과를 만들어내지 말고 "처리 중" 을 알린다. 진행 중인 건에 대해 성공 응답을 주면 클라이언트가 완료로 오인한다. */
  private TransferResult replayResult(TransferOrderEntity order) {
    TransferOrderEntity.Status status = order.getStatus();
    if (status == TransferOrderEntity.Status.UNKNOWN) {
      throw new BusinessException(
          ErrorCode.TRANSFER_RESULT_UNKNOWN, AppMessages.Transfer.RESULT_UNKNOWN);
    }
    if (!status.isTerminal()) {
      throw new BusinessException(ErrorCode.TRANSFER_IN_PROGRESS, AppMessages.Transfer.IN_PROGRESS);
    }
    return toResult(order);
  }

  private static String normalizeKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      // 클라이언트가 키를 안 준 경우. 서버가 만들면 재요청 시 중복 방지는 안 되지만,
      // 최소한 주문 행이 유니크하게 생성되어 상태 추적은 가능하다.
      return "auto-" + UUID.randomUUID();
    }
    String trimmed = idempotencyKey.trim();
    if (trimmed.length() > 64) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "멱등키는 64자를 넘을 수 없습니다.");
    }
    return trimmed;
  }

  private static String extractTransactionId(Map<String, Object> response) {
    if (response == null) {
      return UUID.randomUUID().toString();
    }
    Object txnId = response.get("transactionId");
    if (txnId == null) {
      txnId = response.get("bankTranId");
    }
    if (txnId == null) {
      txnId = response.get("bank_tran_id");
    }
    return txnId != null ? String.valueOf(txnId) : UUID.randomUUID().toString();
  }

  private static void validateTransferInput(
      String userId, String fromAccount, String toAccount, long amount) {
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.INVALID_USER_ID);
    }
    try {
      Long.parseLong(userId);
    } catch (NumberFormatException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.INVALID_USER_ID);
    }
    if (fromAccount == null || fromAccount.isBlank() || toAccount == null || toAccount.isBlank()) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.INVALID_ACCOUNT);
    }
    if (amount <= 0) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.INVALID_AMOUNT);
    }
  }

  private static TransferResult toResult(TransferOrderEntity order) {
    String status =
        order.getStatus() == TransferOrderEntity.Status.EXECUTED
            ? "COMPLETED"
            : order.getStatus().name();
    return new TransferResult(
        order.getBankTransactionId() != null
            ? order.getBankTransactionId()
            : String.valueOf(order.getOrderId()),
        status,
        false);
  }

  @Override
  @Transactional
  public void updateLimit(String userId, long newLimit) {
    if (newLimit < 0) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, AppMessages.Transfer.LIMIT_NEGATIVE);
    }
    UserSettingEntity setting =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, LIMIT_KEY)
            .orElseGet(UserSettingEntity::new);
    setting.setUserId(userId);
    setting.setSettingKey(LIMIT_KEY);
    setting.setSettingValueLong(newLimit);
    userSettingRepository.save(setting);
    auditLogger.logSuccess(userId, "UPDATE_LIMIT", "LIMIT", "newLimit=" + newLimit);
  }

  @Override
  public long getLimit(String userId) {
    return transferLimitGuard.resolveDailyLimit(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public TransferResult getTransfer(String userId, String transferId) {
    long numericUserId = Long.parseLong(userId);
    return transferOrderRepository
        .findByBankTransactionId(transferId)
        .or(() -> parseLong(transferId).flatMap(transferOrderRepository::findWithUserByOrderId))
        // 소유자 검증. 이게 없으면 ID 추측만으로 남의 이체 내역이 노출된다.
        .filter(order -> order.getUser() != null && numericUserId == order.getUser().getUserId())
        .map(TransferService::toResult)
        .orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TransferResult> getTransfers(String userId) {
    return transferOrderRepository
        .findByUser_UserIdOrderByOrderIdDesc(Long.parseLong(userId))
        .stream()
        .map(TransferService::toResult)
        .toList();
  }

  private static java.util.Optional<Long> parseLong(String value) {
    try {
      return java.util.Optional.of(Long.parseLong(value));
    } catch (NumberFormatException e) {
      return java.util.Optional.empty();
    }
  }
}
