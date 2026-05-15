package com.burty.application.port.in;

import com.burty.domain.entity.TransactionEntity;

import java.time.LocalDate;
import java.util.List;

public interface TransactionSyncUseCase {

    int syncFromOpenBanking(String userId, String fintechUseNum);

    List<TransactionEntity> recent(String userId, LocalDate from, LocalDate to);

    int recategorizeAll(String userId);
}
