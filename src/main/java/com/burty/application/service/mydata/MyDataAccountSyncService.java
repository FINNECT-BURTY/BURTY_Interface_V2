package com.burty.application.service.mydata;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.domain.finance.entity.AccountEntity;
import com.burty.domain.finance.entity.AccountEntity.AccountType;
import com.burty.domain.finance.repository.AccountRepository;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.entity.LinkedInstitutionEntity.LinkStatus;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import com.burty.util.AccountNumberHasher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyDataAccountSyncService {

  private static final String FINTECH_USE_NUM_KEY = "FINTECH_USE_NUM";

  private final OpenBankingPort openBankingPort;
  private final LinkedInstitutionRepository linkedInstitutionRepository;
  private final MyDataTokenHydrationService tokenHydrationService;
  private final AccountRepository accountRepository;
  private final UserSettingRepository userSettingRepository;
  private final AccountNumberHasher accountNumberHasher;
  private final MyDataTransmissionLogService transmissionLogService;

  @Transactional
  public int syncFromOpenBanking(String userId) {
    tokenHydrationService.hydrateOpenBanking(userId);

    Long numericUserId = Long.parseLong(userId);
    LinkedInstitutionEntity link =
        linkedInstitutionRepository
            .findByUser_UserIdAndInstitutionCode(
                numericUserId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)
            .filter(l -> l.getStatus() == LinkStatus.ACTIVE)
            .orElse(null);
    if (link == null) {
      transmissionLogService.logInbound(
          userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE, "ACCOUNT_SYNC", "no link");
      return 0;
    }

    Map<String, Object> response = openBankingPort.getAccounts(userId);
    Object accountsObj = response.get("accounts");
    if (!(accountsObj instanceof List<?> accounts) || accounts.isEmpty()) {
      return 0;
    }

    int saved = 0;
    boolean first = true;
    for (Object item : accounts) {
      if (!(item instanceof Map<?, ?> raw)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> account = (Map<String, Object>) raw;
      String fintechUseNum = stringValue(account.get("fintechUseNum"));
      String masked = stringValue(account.get("accountMasked"));
      if (fintechUseNum == null || fintechUseNum.isBlank()) {
        continue;
      }
      if (first) {
        saveFintechUseNum(userId, fintechUseNum);
      }
      String hash = accountNumberHasher.hash(fintechUseNum);
      AccountEntity entity =
          accountRepository
              .findByLinkedInstitution_LinkIdAndAccountNoHash(link.getLinkId(), hash)
              .orElseGet(AccountEntity::new);
      entity.setLinkedInstitution(link);
      entity.setAccountNo(fintechUseNum);
      entity.setAccountNoHash(hash);
      entity.setAccountNoMasked(masked != null ? masked : accountNumberHasher.mask(fintechUseNum));
      entity.setAccountName(stringValue(account.get("bankName")));
      entity.setAccountType(AccountType.DEPOSIT);
      entity.setCurrency("KRW");
      entity.setIsPrimary(first);
      if (entity.getFirstSyncedAt() == null) {
        entity.setFirstSyncedAt(LocalDateTime.now());
      }
      accountRepository.save(entity);
      first = false;
      saved++;
    }

    link.setLastSyncedAt(LocalDateTime.now());
    linkedInstitutionRepository.save(link);
    transmissionLogService.logInbound(
        userId,
        LinkedInstitutionPersistenceService.OPEN_BANKING_CODE,
        "ACCOUNT_SYNC",
        "saved=" + saved);
    return saved;
  }

  private void saveFintechUseNum(String userId, String fintechUseNum) {
    UserSettingEntity setting =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, FINTECH_USE_NUM_KEY)
            .orElseGet(UserSettingEntity::new);
    setting.setUserId(userId);
    setting.setSettingKey(FINTECH_USE_NUM_KEY);
    setting.setSettingValueStr(fintechUseNum);
    userSettingRepository.save(setting);
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
