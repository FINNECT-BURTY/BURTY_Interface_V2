package com.nuri.application.service;

import com.nuri.application.port.in.ExternalFinanceUseCase;
import com.nuri.application.port.out.HanaBankPort;
import com.nuri.application.port.out.ImBankPort;
import com.nuri.application.port.out.KakaoBankPort;
import com.nuri.application.port.out.KbBankPort;
import com.nuri.application.port.out.PensionPort;
import com.nuri.application.port.out.ShinhanBankPort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExternalFinanceService implements ExternalFinanceUseCase {
    private final KakaoBankPort kakaoBankPort;
    private final HanaBankPort hanaBankPort;
    private final KbBankPort kbBankPort;
    private final ShinhanBankPort shinhanBankPort;
    private final ImBankPort imBankPort;
    private final PensionPort pensionPort;

    public ExternalFinanceService(KakaoBankPort kakaoBankPort, HanaBankPort hanaBankPort,
                                  KbBankPort kbBankPort, ShinhanBankPort shinhanBankPort,
                                  ImBankPort imBankPort, PensionPort pensionPort) {
        this.kakaoBankPort = kakaoBankPort;
        this.hanaBankPort = hanaBankPort;
        this.kbBankPort = kbBankPort;
        this.shinhanBankPort = shinhanBankPort;
        this.imBankPort = imBankPort;
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
    public Map<String, Object> getPensionSummary(String userId) {
        return pensionPort.getSummary(userId);
    }
}
