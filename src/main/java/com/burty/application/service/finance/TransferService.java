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
import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.security.BiometricAuthPort;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.constant.AppMessages;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.entity.TransferRecordEntity;
import com.burty.domain.finance.model.TransferResult;
import com.burty.domain.finance.repository.RegisteredAccountRepository;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.finance.repository.TransferRecordRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.domain.user.repository.UserSettingRepository;
import com.burty.util.AccountNumberHasher;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {

  private static final long FAMILY_ALERT_THRESHOLD = 1_000_000L;
  private static final long LARGE_TRANSFER_THRESHOLD = 3_000_000L;
  private static final String LIMIT_KEY = "TRANSFER_LIMIT";
  private static final String DEFAULT_BANK_CODE = "000";

  private final BiometricAuthPort biometricAuthPort;
  private final FamilyAlertPort familyAlertPort;
  private final AuditLogger auditLogger;
  private final TransferRecordRepository transferRecordRepository;
  private final TransferOrderRepository transferOrderRepository;
  private final RegisteredAccountRepository registeredAccountRepository;
  private final UserSettingRepository userSettingRepository;
  private final UserRepository userRepository;
  private final AccountNumberHasher accountNumberHasher;
  private final TransferLimitGuard transferLimitGuard;
  private final OpenBankingPort openBankingPort;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;

  @Override
  @Transactional
  public TransferResult transfer(
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String idempotencyKey) {
    validateTransferInput(userId, fromAccount, toAccount, amount);

    String effectiveKey =
        idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    long numericUserId = Long.parseLong(userId);

    if (effectiveKey != null) {
      var existingOrder =
          transferOrderRepository.findByUser_UserIdAndIdempotencyKey(numericUserId, effectiveKey);
      if (existingOrder.isPresent()) {
        return toResult(existingOrder.get());
      }
      return transferRecordRepository
          .findByUserIdAndIdempotencyKey(userId, effectiveKey)
          .map(
              existing ->
                  new TransferResult(
                      existing.getTransferId(),
                      existing.getStatus(),
                      Boolean.TRUE.equals(existing.getFamilyNotified())))
          .orElseGet(
              () ->
                  executeTransfer(
                      userId,
                      fromAccount,
                      toAccount,
                      amount,
                      description,
                      assertionToken,
                      effectiveKey));
    }
    return executeTransfer(
        userId, fromAccount, toAccount, amount, description, assertionToken, null);
  }

  private TransferResult executeTransfer(
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String assertionToken,
      String idempotencyKey) {
    transferLimitGuard.assertWithinLimit(userId, amount);

    if (linkedInstitutionPersistence
        .loadTokenBundle(userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)
        .isEmpty()) {
      throw new BusinessException(
          ErrorCode.DATA_NOT_FOUND, AppMessages.Transfer.OPENBANKING_NOT_LINKED);
    }
    tokenHydrationService.hydrateOpenBanking(userId);

    TransferOrderEntity order =
        createOrder(userId, fromAccount, toAccount, amount, description, idempotencyKey);
    order.setStatus(TransferOrderEntity.Status.AUTH_REQUESTED);
    transferOrderRepository.save(order);

    if (!biometricAuthPort.verifyAssertion(userId, assertionToken)) {
      order.setStatus(TransferOrderEntity.Status.FAILED);
      order.setFailedReason(AppMessages.Transfer.WEBAUTHN_VERIFY_FAILED);
      transferOrderRepository.save(order);
      throw new BusinessException(ErrorCode.FORBIDDEN, AppMessages.Transfer.WEBAUTHN_VERIFY_FAILED);
    }

    order.setStatus(TransferOrderEntity.Status.AUTHORIZED);
    order.setStatus(TransferOrderEntity.Status.EXECUTING);
    transferOrderRepository.save(order);

    try {
      Map<String, Object> obResponse =
          openBankingPort.transfer(userId, fromAccount, toAccount, amount, idempotencyKey);
      String bankTxnId = extractTransactionId(obResponse);

      boolean notify = processFamilyAlerts(userId, toAccount, amount);
      auditLogger.logSuccess(
          userId, "TRANSFER", toAccount, "amount=" + amount + ", description=" + description);

      TransferResult result = new TransferResult(bankTxnId, "COMPLETED", notify);
      TransferRecordEntity record = new TransferRecordEntity();
      record.setTransferId(result.transferId());
      record.setUserId(userId);
      record.setFromAccount(accountNumberHasher.mask(fromAccount));
      record.setToAccount(accountNumberHasher.mask(toAccount));
      record.setAmount(amount);
      record.setStatus(result.status());
      record.setFamilyNotified(notify);
      record.setDescription(description);
      record.setIdempotencyKey(idempotencyKey);
      transferRecordRepository.save(record);

      transferLimitGuard.recordUsage(userId, amount);

      order.setStatus(TransferOrderEntity.Status.EXECUTED);
      order.setExecutedAt(LocalDateTime.now());
      order.setBankTransactionId(bankTxnId);
      transferOrderRepository.save(order);
      return result;
    } catch (RuntimeException e) {
      order.setStatus(TransferOrderEntity.Status.FAILED);
      order.setFailedReason(AppMessages.Transfer.ORDER_FAILED_PREFIX + e.getMessage());
      transferOrderRepository.save(order);
      throw e;
    }
  }

  private static String extractTransactionId(Map<String, Object> response) {
    if (response == null) {
      return UUID.randomUUID().toString();
    }
    Object txnId = response.get("transactionId");
    if (txnId == null) {
      txnId = response.get("bankTranId");
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

  private TransferOrderEntity createOrder(
      String userId,
      String fromAccount,
      String toAccount,
      long amount,
      String description,
      String idempotencyKey) {
    UserEntity user =
        userRepository
            .findById(Long.parseLong(userId))
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.USER_NOT_FOUND, AppMessages.Transfer.USER_NOT_FOUND));

    TransferOrderEntity order = new TransferOrderEntity();
    order.setUser(user);
    order.setIdempotencyKey(idempotencyKey == null ? "auto-" + UUID.randomUUID() : idempotencyKey);
    order.setToAccountNo(toAccount);
    order.setToAccountNoMasked(accountNumberHasher.mask(toAccount));
    order.setToBankCode(DEFAULT_BANK_CODE);
    order.setAmount(amount);
    order.setMemo(description);
    order.setStatus(TransferOrderEntity.Status.PENDING);
    return order;
  }

  private boolean processFamilyAlerts(String userId, String toAccount, long amount) {
    boolean notify = amount >= FAMILY_ALERT_THRESHOLD;
    boolean unusualNightTransfer =
        LocalTime.now().isAfter(LocalTime.of(23, 0))
            || LocalTime.now().isBefore(LocalTime.of(6, 0));
    boolean unregisteredAccountTransfer =
        !registeredAccountRepository.existsByUserIdAndAccountNo(userId, toAccount);
    boolean largeTransfer = amount >= LARGE_TRANSFER_THRESHOLD;

    if (unusualNightTransfer || unregisteredAccountTransfer || largeTransfer) {
      familyAlertPort.send(userId, AppMessages.Transfer.FAMILY_ALERT_SUSPICIOUS);
      notify = true;
    }
    if (notify) {
      familyAlertPort.send(
          userId, AppMessages.Transfer.FAMILY_ALERT_TRANSFER.formatted(amount, toAccount));
    }
    return notify;
  }

  private TransferResult toResult(TransferOrderEntity order) {
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
  public TransferResult getTransfer(String transferId) {
    return transferRecordRepository
        .findById(transferId)
        .map(
            r ->
                new TransferResult(
                    r.getTransferId(), r.getStatus(), Boolean.TRUE.equals(r.getFamilyNotified())))
        .orElse(null);
  }

  @Override
  public List<TransferResult> getTransfers(String userId) {
    return transferRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(
            r ->
                new TransferResult(
                    r.getTransferId(), r.getStatus(), Boolean.TRUE.equals(r.getFamilyNotified())))
        .toList();
  }
}
