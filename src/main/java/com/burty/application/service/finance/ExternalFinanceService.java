/**
 *
 *
 * <pre>
 * <b>Description  : 금융 애플리케이션 서비스 (ExternalFinanceService)</b>
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

import com.burty.adapter.out.store.IdempotencyStore;
import com.burty.application.dto.finance.ExternalTransferResponse;
import com.burty.application.dto.finance.OpenBankingAccountsResponse;
import com.burty.application.dto.finance.OpenBankingAuthorizeResponse;
import com.burty.application.dto.finance.OpenBankingBalanceResponse;
import com.burty.application.dto.finance.OpenBankingTransactionsResponse;
import com.burty.application.dto.finance.PensionSummaryResponse;
import com.burty.application.port.in.finance.ExternalFinanceUseCase;
import com.burty.application.port.out.bank.HanaBankPort;
import com.burty.application.port.out.bank.ImBankPort;
import com.burty.application.port.out.bank.KakaoBankPort;
import com.burty.application.port.out.bank.KbBankPort;
import com.burty.application.port.out.bank.OpenBankingOAuthPort;
import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.bank.PensionPort;
import com.burty.application.port.out.bank.ShinhanBankPort;
import com.burty.application.service.mydata.FinanceOAuthStateService;
import com.burty.application.service.mydata.LinkedInstitutionPersistenceService;
import com.burty.application.service.mydata.MyDataAccountSyncService;
import com.burty.application.service.mydata.MyDataTokenHydrationService;
import com.burty.domain.mydata.model.MyDataTokenBundle;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalFinanceService implements ExternalFinanceUseCase {
  private final KakaoBankPort kakaoBankPort;
  private final HanaBankPort hanaBankPort;
  private final KbBankPort kbBankPort;
  private final ShinhanBankPort shinhanBankPort;
  private final ImBankPort imBankPort;
  private final OpenBankingPort openBankingPort;
  private final OpenBankingOAuthPort openBankingOAuthPort;
  private final PensionPort pensionPort;
  private final IdempotencyStore idempotencyStore;
  private final MyDataAccountSyncService accountSyncService;
  private final FinanceOAuthStateService financeOAuthStateService;
  private final LinkedInstitutionPersistenceService linkedInstitutionPersistence;
  private final MyDataTokenHydrationService tokenHydrationService;
  private static final long IDEMPOTENCY_TTL_SECONDS = 86_400L;

  public ExternalFinanceService(
      KakaoBankPort kakaoBankPort,
      HanaBankPort hanaBankPort,
      KbBankPort kbBankPort,
      ShinhanBankPort shinhanBankPort,
      ImBankPort imBankPort,
      OpenBankingPort openBankingPort,
      OpenBankingOAuthPort openBankingOAuthPort,
      PensionPort pensionPort,
      IdempotencyStore idempotencyStore,
      MyDataAccountSyncService accountSyncService,
      FinanceOAuthStateService financeOAuthStateService,
      LinkedInstitutionPersistenceService linkedInstitutionPersistence,
      MyDataTokenHydrationService tokenHydrationService) {
    this.kakaoBankPort = kakaoBankPort;
    this.hanaBankPort = hanaBankPort;
    this.kbBankPort = kbBankPort;
    this.shinhanBankPort = shinhanBankPort;
    this.imBankPort = imBankPort;
    this.openBankingPort = openBankingPort;
    this.openBankingOAuthPort = openBankingOAuthPort;
    this.pensionPort = pensionPort;
    this.idempotencyStore = idempotencyStore;
    this.accountSyncService = accountSyncService;
    this.financeOAuthStateService = financeOAuthStateService;
    this.linkedInstitutionPersistence = linkedInstitutionPersistence;
    this.tokenHydrationService = tokenHydrationService;
  }

  @Override
  public ExternalTransferResponse transferToKakaoBank(
      String userId, String toAccount, long amount) {
    return ExternalTransferResponse.fromMap(kakaoBankPort.transfer(userId, toAccount, amount));
  }

  @Override
  public ExternalTransferResponse transferToHanaBank(String userId, String toAccount, long amount) {
    return ExternalTransferResponse.fromMap(hanaBankPort.transfer(userId, toAccount, amount));
  }

  @Override
  public ExternalTransferResponse transferToKbBank(String userId, String toAccount, long amount) {
    return ExternalTransferResponse.fromMap(kbBankPort.transfer(userId, toAccount, amount));
  }

  @Override
  public ExternalTransferResponse transferToShinhanBank(
      String userId, String toAccount, long amount) {
    return ExternalTransferResponse.fromMap(shinhanBankPort.transfer(userId, toAccount, amount));
  }

  @Override
  public ExternalTransferResponse transferToImBank(String userId, String toAccount, long amount) {
    return ExternalTransferResponse.fromMap(imBankPort.transfer(userId, toAccount, amount));
  }

  @Override
  public OpenBankingAccountsResponse getOpenBankingAccounts(String userId) {
    tokenHydrationService.hydrateOpenBanking(userId);
    return OpenBankingAccountsResponse.fromMap(openBankingPort.getAccounts(userId));
  }

  @Override
  public OpenBankingBalanceResponse getOpenBankingBalance(String userId, String fintechUseNum) {
    tokenHydrationService.hydrateOpenBanking(userId);
    return OpenBankingBalanceResponse.fromMap(openBankingPort.getBalance(userId, fintechUseNum));
  }

  @Override
  public OpenBankingTransactionsResponse getOpenBankingTransactions(
      String userId, String fintechUseNum) {
    tokenHydrationService.hydrateOpenBanking(userId);
    return OpenBankingTransactionsResponse.fromMap(
        openBankingPort.getTransactions(userId, fintechUseNum));
  }

  @Override
  public ExternalTransferResponse transferByOpenBanking(
      String userId, String fromAccount, String toAccount, long amount, String idempotencyKey) {
    tokenHydrationService.hydrateOpenBanking(userId);
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return ExternalTransferResponse.fromMap(
          openBankingPort.transfer(userId, fromAccount, toAccount, amount, null));
    }
    String key = userId + "|" + idempotencyKey;
    return idempotencyStore
        .get(key)
        .map(ExternalTransferResponse::fromMap)
        .orElseGet(
            () -> {
              Map<String, Object> response =
                  openBankingPort.transfer(userId, fromAccount, toAccount, amount, idempotencyKey);
              idempotencyStore.put(key, response, IDEMPOTENCY_TTL_SECONDS);
              return ExternalTransferResponse.fromMap(response);
            });
  }

  @Override
  public PensionSummaryResponse getPensionSummary(String userId) {
    return PensionSummaryResponse.fromMap(pensionPort.getSummary(userId));
  }

  @Override
  public OpenBankingAuthorizeResponse createOpenBankingAuthorize(String userId) {
    String oauthState =
        financeOAuthStateService.issue(
            FinanceOAuthStateService.PROVIDER_OPENBANKING,
            userId,
            LinkedInstitutionPersistenceService.OPEN_BANKING_CODE);
    boolean linked =
        linkedInstitutionPersistence
            .loadTokenBundle(userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE)
            .isPresent();
    return new OpenBankingAuthorizeResponse(
        openBankingOAuthPort.buildAuthorizeUrl(oauthState),
        linked || openBankingOAuthPort.isLinked(userId));
  }

  @Override
  @Transactional
  public boolean exchangeOpenBankingAuthorizationCode(String userId, String code) {
    return exchangeForUser(userId, code);
  }

  @Override
  @Transactional
  public boolean exchangeOpenBankingAuthorizationCodeByState(String state, String code) {
    FinanceOAuthStateService.FinanceOAuthContext ctx =
        financeOAuthStateService.consume(FinanceOAuthStateService.PROVIDER_OPENBANKING, state);
    return exchangeForUser(ctx.userId(), code);
  }

  private boolean exchangeForUser(String userId, String code) {
    MyDataTokenBundle bundle = openBankingOAuthPort.exchangeAuthorizationCode(userId, code);
    if (bundle == null || bundle.accessToken() == null || bundle.accessToken().isBlank()) {
      return false;
    }
    linkedInstitutionPersistence.saveTokens(
        userId, LinkedInstitutionPersistenceService.OPEN_BANKING_CODE, bundle);
    tokenHydrationService.hydrateOpenBanking(userId);
    accountSyncService.syncFromOpenBanking(userId);
    return true;
  }
}
