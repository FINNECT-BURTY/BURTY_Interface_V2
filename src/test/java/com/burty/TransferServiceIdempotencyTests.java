/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (TransferServiceIdempotencyTests)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
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
package com.burty;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.notify.FamilyAlertPort;
import com.burty.application.port.out.security.BiometricAuthPort;
import com.burty.application.service.finance.TransferLimitGuard;
import com.burty.application.service.finance.TransferService;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.application.service.support.AuditLogger;
import com.burty.domain.finance.model.TransferResult;
import com.burty.domain.finance.repository.RegisteredAccountRepository;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.finance.repository.TransferRecordRepository;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.UserRepository;
import com.burty.domain.user.repository.UserSettingRepository;
import com.burty.util.AccountNumberHasher;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TransferServiceIdempotencyTests {

  private TransferService transferService;
  private TransferRecordRepository transferRecordRepository;

  @BeforeEach
  void setUp() {
    BiometricAuthPort biometricAuthPort = (userId, token) -> true;
    FamilyAlertPort familyAlertPort = Mockito.mock(FamilyAlertPort.class);
    AuditLogger auditLogger = Mockito.mock(AuditLogger.class);
    transferRecordRepository = Mockito.mock(TransferRecordRepository.class);
    TransferOrderRepository transferOrderRepository = Mockito.mock(TransferOrderRepository.class);
    RegisteredAccountRepository registeredAccountRepository =
        Mockito.mock(RegisteredAccountRepository.class);
    UserSettingRepository userSettingRepository = Mockito.mock(UserSettingRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    TransferLimitGuard transferLimitGuard = Mockito.mock(TransferLimitGuard.class);
    OpenBankingPort openBankingPort = Mockito.mock(OpenBankingPort.class);
    LinkedInstitutionPersistenceService linkedInstitutionPersistence =
        Mockito.mock(LinkedInstitutionPersistenceService.class);
    MyDataTokenHydrationService tokenHydrationService =
        Mockito.mock(MyDataTokenHydrationService.class);
    AccountNumberHasher accountNumberHasher = new AccountNumberHasher();

    UserEntity user = new UserEntity();
    user.setUserId(1L);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(
            transferOrderRepository.findByUser_UserIdAndIdempotencyKey(
                Mockito.eq(1L), Mockito.anyString()))
        .thenReturn(Optional.empty());
    Mockito.when(
            linkedInstitutionPersistence.loadTokenBundle(
                Mockito.eq("1"), Mockito.eq(LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)))
        .thenReturn(
            Optional.of(
                new MyDataTokenBundle("access", "refresh", LocalDateTime.now().plusHours(1))));
    Mockito.when(
            openBankingPort.transfer(
                Mockito.eq("1"),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.any()))
        .thenReturn(Map.of("transactionId", "ob-tx-1", "status", "ACCEPTED"));

    transferService =
        new TransferService(
            biometricAuthPort,
            familyAlertPort,
            auditLogger,
            transferRecordRepository,
            transferOrderRepository,
            registeredAccountRepository,
            userSettingRepository,
            userRepository,
            accountNumberHasher,
            transferLimitGuard,
            openBankingPort,
            linkedInstitutionPersistence,
            tokenHydrationService);

    Mockito.when(registeredAccountRepository.existsByUserIdAndAccountNo("1", "222"))
        .thenReturn(true);
  }

  @Test
  void sameIdempotencyKeyReturnsExistingTransferWithoutSecondSave() {
    Mockito.when(transferRecordRepository.findByUserIdAndIdempotencyKey("1", "key-1"))
        .thenReturn(java.util.Optional.of(record("tx-existing", "1", "key-1", true)));

    TransferResult result =
        transferService.transfer("1", "111", "222", 1000L, "test", "assertion", "key-1");

    Assertions.assertEquals("tx-existing", result.transferId());
    Mockito.verify(transferRecordRepository, Mockito.never()).save(Mockito.any());
  }

  @Test
  void newIdempotencyKeyPersistsTransfer() {
    Mockito.when(transferRecordRepository.findByUserIdAndIdempotencyKey("1", "key-2"))
        .thenReturn(java.util.Optional.empty());

    TransferResult result =
        transferService.transfer("1", "111", "222", 1000L, "test", "assertion", "key-2");

    Assertions.assertEquals("COMPLETED", result.status());
    Mockito.verify(transferRecordRepository).save(Mockito.any());
  }

  private static com.burty.domain.finance.entity.TransferRecordEntity record(
      String transferId, String userId, String idempotencyKey, boolean familyNotified) {
    com.burty.domain.finance.entity.TransferRecordEntity entity =
        new com.burty.domain.finance.entity.TransferRecordEntity();
    entity.setTransferId(transferId);
    entity.setUserId(userId);
    entity.setIdempotencyKey(idempotencyKey);
    entity.setStatus("COMPLETED");
    entity.setFamilyNotified(familyNotified);
    entity.setAmount(1000L);
    return entity;
  }
}
