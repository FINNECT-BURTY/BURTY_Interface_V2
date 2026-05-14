package com.berty.application.service;

import com.berty.application.port.in.TransactionSyncUseCase;
import com.berty.application.port.out.OpenBankingPort;
import com.berty.domain.entity.TransactionEntity;
import com.berty.domain.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionSyncService implements TransactionSyncUseCase {
    private static final Logger log = LoggerFactory.getLogger(TransactionSyncService.class);

    private final OpenBankingPort openBankingPort;
    private final TransactionRepository transactionRepository;
    private final TransactionCategorizer categorizer;

    public TransactionSyncService(OpenBankingPort openBankingPort,
                                  TransactionRepository transactionRepository,
                                  TransactionCategorizer categorizer) {
        this.openBankingPort = openBankingPort;
        this.transactionRepository = transactionRepository;
        this.categorizer = categorizer;
    }

    @Override
    @Transactional
    public int syncFromOpenBanking(String userId, String fintechUseNum) {
        UUID userUuid = parseUuid(userId);
        if (userUuid == null) {
            log.warn("syncFromOpenBanking skipped: userId is not a UUID userId={}", userId);
            return 0;
        }
        Map<String, Object> response = openBankingPort.getTransactions(userId, fintechUseNum);
        Object txObj = response.get("transactions");
        if (!(txObj instanceof List<?> txList)) return 0;

        int saved = 0;
        for (Object item : txList) {
            if (!(item instanceof Map<?, ?> txMap)) continue;
            String externalId = stringValue(txMap.get("id"));
            if (externalId == null || externalId.isBlank()) {
                externalId = fintechUseNum + "-" + stringValue(txMap.get("date")) + "-" + stringValue(txMap.get("amount"));
            }
            if (transactionRepository.findByUserIdAndExternalTxId(userUuid, externalId).isPresent()) continue;

            String type = stringValue(txMap.get("type"));
            String direction = "WITHDRAWAL".equalsIgnoreCase(type) || "OUT".equalsIgnoreCase(type) ? "OUT" : "IN";
            long amount = longValue(txMap.get("amount"), 0L);
            if (amount == 0) continue;

            TransactionEntity tx = new TransactionEntity();
            tx.setUserId(userUuid);
            tx.setExternalTxId(externalId);
            tx.setTxnDate(parseDate(stringValue(txMap.get("date"))));
            tx.setAmount(Math.abs(amount));
            tx.setDirection(direction);
            tx.setMerchant(stringValue(txMap.get("merchant")));
            tx.setMemo(stringValue(txMap.get("memo")));
            tx.setSource("OPEN_BANKING");
            categorizer.categorize(tx);
            transactionRepository.save(tx);
            saved++;
        }
        log.info("Transaction sync userId={} fintechUseNum={} saved={}", userId, fintechUseNum, saved);
        return saved;
    }

    @Override
    public List<TransactionEntity> recent(String userId, LocalDate from, LocalDate to) {
        UUID userUuid = parseUuid(userId);
        if (userUuid == null) return List.of();
        if (from == null && to == null) return transactionRepository.findByUserIdOrderByTxnDateDesc(userUuid);
        LocalDate effectiveFrom = from == null ? LocalDate.now().minusMonths(3) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        return transactionRepository.findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(userUuid, effectiveFrom, effectiveTo);
    }

    @Override
    @Transactional
    public int recategorizeAll(String userId) {
        UUID userUuid = parseUuid(userId);
        if (userUuid == null) return 0;
        List<TransactionEntity> all = transactionRepository.findByUserIdOrderByTxnDateDesc(userUuid);
        List<TransactionEntity> changed = new ArrayList<>();
        for (TransactionEntity tx : all) {
            String prevExpense = tx.getExpenseCategoryCode();
            String prevIncome = tx.getIncomeCategoryCode();
            tx.setExpenseCategoryCode(null);
            tx.setIncomeCategoryCode(null);
            categorizer.categorize(tx);
            if (!eq(prevExpense, tx.getExpenseCategoryCode()) || !eq(prevIncome, tx.getIncomeCategoryCode())) {
                changed.add(tx);
            }
        }
        if (!changed.isEmpty()) transactionRepository.saveAll(changed);
        return changed.size();
    }

    private boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private String stringValue(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private long longValue(Object o, long fallback) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return fallback; }
        }
        return fallback;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(s); }
        catch (Exception e) {
            try { return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")); }
            catch (Exception ex) { return LocalDate.now(); }
        }
    }

    private UUID parseUuid(String userId) {
        if (userId == null || userId.length() < 32) return null;
        try { return UUID.fromString(userId); }
        catch (IllegalArgumentException e) { return null; }
    }
}
