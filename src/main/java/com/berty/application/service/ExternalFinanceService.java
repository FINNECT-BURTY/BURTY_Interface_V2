package com.berty.application.service;

import com.berty.application.port.in.ExternalFinanceUseCase;
import com.berty.application.port.out.HanaBankPort;
import com.berty.application.port.out.ImBankPort;
import com.berty.application.port.out.KakaoBankPort;
import com.berty.application.port.out.KbBankPort;
import com.berty.application.port.out.OpenBankingPort;
import com.berty.application.port.out.PensionPort;
import com.berty.application.port.out.ShinhanBankPort;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ExternalFinanceService implements ExternalFinanceUseCase {
    private final KakaoBankPort kakaoBankPort;
    private final HanaBankPort hanaBankPort;
    private final KbBankPort kbBankPort;
    private final ShinhanBankPort shinhanBankPort;
    private final ImBankPort imBankPort;
    private final OpenBankingPort openBankingPort;
    private final PensionPort pensionPort;
    private final Map<String, Map<String, Object>> openBankingTransferIdempotencyStore = new ConcurrentHashMap<>();

    public ExternalFinanceService(KakaoBankPort kakaoBankPort, HanaBankPort hanaBankPort, KbBankPort kbBankPort, ShinhanBankPort shinhanBankPort, ImBankPort imBankPort, OpenBankingPort openBankingPort, PensionPort pensionPort) {
        this.kakaoBankPort = kakaoBankPort;
        this.hanaBankPort = hanaBankPort;
        this.kbBankPort = kbBankPort;
        this.shinhanBankPort = shinhanBankPort;
        this.imBankPort = imBankPort;
        this.openBankingPort = openBankingPort;
        this.pensionPort = pensionPort;
    }

    @Override
    public Map<String, Object> transferToKakaoBank(String userId, String toAccount, long amount) {
        return kakaoBankPort.transfer(userId, toAccount, amount);
    }

    @Override
    public Map<String, Object> transferToHanaBank(String userId, String toAccount, long amount) {
        return hanaBankPort.transfer(userId, toAccount, amount);
    }

    @Override
    public Map<String, Object> transferToKbBank(String userId, String toAccount, long amount) {
        return kbBankPort.transfer(userId, toAccount, amount);
    }

    @Override
    public Map<String, Object> transferToShinhanBank(String userId, String toAccount, long amount) {
        return shinhanBankPort.transfer(userId, toAccount, amount);
    }

    @Override
    public Map<String, Object> transferToImBank(String userId, String toAccount, long amount) {
        return imBankPort.transfer(userId, toAccount, amount);
    }

    @Override
    public Map<String, Object> getOpenBankingAccounts(String userId) {
        return openBankingPort.getAccounts(userId);
    }

    @Override
    public Map<String, Object> getOpenBankingBalance(String userId, String fintechUseNum) {
        return openBankingPort.getBalance(userId, fintechUseNum);
    }

    @Override
    public Map<String, Object> getOpenBankingTransactions(String userId, String fintechUseNum) {
        return openBankingPort.getTransactions(userId, fintechUseNum);
    }

    @Override
    public Map<String, Object> transferByOpenBanking(String userId, String fromAccount, String toAccount, long amount, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return openBankingPort.transfer(userId, fromAccount, toAccount, amount, null);
        }
        String key = userId + "|" + idempotencyKey;
        Map<String, Object> existing = openBankingTransferIdempotencyStore.get(key);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> response = openBankingPort.transfer(userId, fromAccount, toAccount, amount, idempotencyKey);
        openBankingTransferIdempotencyStore.put(key, response);
        return response;
    }

    @Override
    public Map<String, Object> getPensionSummary(String userId) {
        return pensionPort.getSummary(userId);
    }
}
