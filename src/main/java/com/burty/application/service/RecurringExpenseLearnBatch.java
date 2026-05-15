package com.burty.application.service;

import com.burty.application.port.out.AuditLogPort;
import com.burty.domain.entity.RecurringExpenseEntity;
import com.burty.domain.entity.TransactionEntity;
import com.burty.domain.model.AuditEvent;
import com.burty.domain.repository.RecurringExpenseRepository;
import com.burty.domain.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 월 1회 거래내역을 분석해 같은 카테고리에서 3회 이상, 표준편차 작은 지출을 RecurringExpenseEntity로 학습.
 * 이미 등록된 항목은 occurrenceCount/avgAmount/dayOfMonth/lastSeenAt만 갱신.
 */
@Component
public class RecurringExpenseLearnBatch {
    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseLearnBatch.class);
    private static final int LOOKBACK_MONTHS = 3;
    private static final int MIN_OCCURRENCES = 3;
    private static final double MAX_RELATIVE_STDDEV = 0.35;

    private final TransactionRepository transactionRepository;
    private final RecurringExpenseRepository recurringExpenseRepository;
    private final AuditLogPort auditLogPort;

    public RecurringExpenseLearnBatch(TransactionRepository transactionRepository,
                                      RecurringExpenseRepository recurringExpenseRepository,
                                      AuditLogPort auditLogPort) {
        this.transactionRepository = transactionRepository;
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.auditLogPort = auditLogPort;
    }

    @Scheduled(cron = "${burty.recurring.learn-cron:0 0 3 1 * *}")
    @Transactional
    public void learnMonthly() {
        LocalDate since = LocalDate.now().minusMonths(LOOKBACK_MONTHS);
        List<UUID> userIds = transactionRepository.findDistinctUserIdsSince(since);
        int totalLearned = 0;
        int totalUsers = 0;
        for (UUID userId : userIds) {
            try {
                int learned = learnForUser(userId, since);
                totalLearned += learned;
                totalUsers++;
            } catch (Exception e) {
                log.warn("Recurring learn failed userId={} err={}", userId, e.getMessage());
            }
        }
        log.info("Recurring expense learn batch: users={} totalLearned={} since={}",
                totalUsers, totalLearned, since);
        auditLogPort.save(new AuditEvent(
                UUID.randomUUID().toString(), "system",
                "RECURRING_LEARN_BATCH", "BATCH", "SUCCESS",
                "users=" + totalUsers + ",learned=" + totalLearned,
                LocalDateTime.now()
        ));
    }

    private int learnForUser(UUID userId, LocalDate since) {
        List<TransactionEntity> txs = transactionRepository
                .findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(userId, since, LocalDate.now());

        Map<String, Aggregator> byCategory = new HashMap<>();
        for (TransactionEntity tx : txs) {
            if (!"OUT".equalsIgnoreCase(tx.getDirection())) continue;
            String category = tx.getExpenseCategoryCode();
            if (category == null) continue;
            byCategory.computeIfAbsent(category, k -> new Aggregator()).add(tx);
        }

        int learned = 0;
        for (Map.Entry<String, Aggregator> entry : byCategory.entrySet()) {
            Aggregator agg = entry.getValue();
            if (agg.count < MIN_OCCURRENCES) continue;
            double mean = agg.sum / (double) agg.count;
            if (mean <= 0) continue;
            double stdDev = Math.sqrt(agg.sumSq / agg.count - mean * mean);
            if (stdDev / mean > MAX_RELATIVE_STDDEV) continue;

            int dayOfMonth = mostFrequentDay(agg);
            String name = mostFrequentName(agg);
            double confidence = Math.max(0.3, Math.min(0.95, 1.0 - (stdDev / mean)));

            RecurringExpenseEntity existing = findExistingForCategory(userId, entry.getKey());
            if (existing == null) {
                existing = new RecurringExpenseEntity();
                existing.setUserId(userId);
                existing.setExpenseCategoryCode(entry.getKey());
                existing.setActive(true);
            }
            existing.setName(name == null ? entry.getKey() : name);
            existing.setAvgAmount(Math.round(mean));
            existing.setDayOfMonth(dayOfMonth);
            existing.setOccurrenceCount(agg.count);
            existing.setConfidence(Math.round(confidence * 100.0) / 100.0);
            existing.setLastSeenAt(agg.latest);
            recurringExpenseRepository.save(existing);
            learned++;
        }
        return learned;
    }

    private RecurringExpenseEntity findExistingForCategory(UUID userId, String categoryCode) {
        return recurringExpenseRepository
                .findByUserIdAndExpenseCategoryCodeAndActiveTrue(userId, categoryCode)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private int mostFrequentDay(Aggregator agg) {
        return agg.dayCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(20);
    }

    private String mostFrequentName(Aggregator agg) {
        return agg.nameCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static final class Aggregator {
        long sum;
        double sumSq;
        int count;
        LocalDateTime latest;
        final Map<Integer, Integer> dayCounts = new HashMap<>();
        final Map<String, Integer> nameCounts = new HashMap<>();

        void add(TransactionEntity tx) {
            long amount = tx.getAmount();
            sum += amount;
            sumSq += (double) amount * amount;
            count++;
            int dom = tx.getTxnDate() == null ? 20 : tx.getTxnDate().getDayOfMonth();
            dayCounts.merge(dom, 1, Integer::sum);
            String label = tx.getMerchant() != null && !tx.getMerchant().isBlank()
                    ? tx.getMerchant()
                    : (tx.getMemo() != null ? tx.getMemo() : null);
            if (label != null && !label.isBlank()) {
                nameCounts.merge(label, 1, Integer::sum);
            }
            LocalDateTime occurredAt = tx.getCreatedAt();
            if (occurredAt != null && (latest == null || occurredAt.isAfter(latest))) {
                latest = occurredAt;
            }
        }
    }
}
