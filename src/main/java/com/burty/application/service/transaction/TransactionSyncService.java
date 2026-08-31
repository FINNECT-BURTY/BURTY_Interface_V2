/**
 *
 *
 * <pre>
 * <b>Description  : 거래 애플리케이션 서비스 (TransactionSyncService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.transaction
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
package com.burty.application.service.transaction;

import com.burty.application.port.in.transaction.TransactionSyncUseCase;
import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.outbox.OutboxPublisher;
import com.burty.application.service.cashflow.BudgetService;
import com.burty.core.constant.LogMessages;
import com.burty.domain.transaction.entity.TransactionEntity;
import com.burty.domain.transaction.repository.TransactionRepository;
import com.burty.util.PiiMasker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionSyncService implements TransactionSyncUseCase {
  private static final Logger log = LoggerFactory.getLogger(TransactionSyncService.class);

  /** 이 금액 이상 출금은 개별 알림 대상. 그 미만은 건수 요약으로만 알린다. */
  private static final long NOTABLE_AMOUNT = 50_000L;

  private final OpenBankingPort openBankingPort;
  private final TransactionRepository transactionRepository;
  private final TransactionCategorizer categorizer;
  private final BudgetService budgetService;
  private final OutboxPublisher outboxPublisher;

  public TransactionSyncService(
      OpenBankingPort openBankingPort,
      TransactionRepository transactionRepository,
      TransactionCategorizer categorizer,
      BudgetService budgetService,
      OutboxPublisher outboxPublisher) {
    this.openBankingPort = openBankingPort;
    this.transactionRepository = transactionRepository;
    this.categorizer = categorizer;
    this.budgetService = budgetService;
    this.outboxPublisher = outboxPublisher;
  }

  /**
   * 신규 거래 알림 발행.
   *
   * <p>거래 건수가 많으면 건별 알림은 소음이다. 큰 금액의 출금만 개별로 알리고, 나머지는 건수 요약으로 한 번만 알린다.
   */
  private void publishTransactionEvents(String userId, List<TransactionEntity> transactions) {
    List<TransactionEntity> notable =
        transactions.stream()
            .filter(tx -> "OUT".equals(tx.getDirection()))
            .filter(tx -> tx.getAmount() != null && tx.getAmount() >= NOTABLE_AMOUNT)
            .toList();

    for (TransactionEntity tx : notable) {
      outboxPublisher.publish(
          "Transaction",
          String.valueOf(tx.getExternalTxId()),
          TransactionNotificationOutboxHandler.EVENT_TYPE,
          Map.of(
              "userId",
              userId,
              "amount",
              tx.getAmount(),
              "merchant",
              tx.getMerchant() == null ? "" : tx.getMerchant(),
              "txnDate",
              String.valueOf(tx.getTxnDate())));
    }

    int remaining = transactions.size() - notable.size();
    if (remaining > 0) {
      outboxPublisher.publish(
          "Transaction",
          userId + ":summary:" + System.identityHashCode(transactions),
          TransactionNotificationOutboxHandler.SUMMARY_EVENT_TYPE,
          Map.of("userId", userId, "count", remaining));
    }
  }

  @Override
  @Transactional
  public int syncFromOpenBanking(String userId, String fintechUseNum) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) {
      log.warn("syncFromOpenBanking skipped: userId is not numeric userId={}", userId);
      return 0;
    }
    Map<String, Object> response = openBankingPort.getTransactions(userId, fintechUseNum);
    Object txObj = response.get("transactions");
    if (!(txObj instanceof List<?> txList)) return 0;

    int saved = 0;
    List<TransactionEntity> newTransactions = new ArrayList<>();
    // 기관이 거래 ID 를 주지 않을 때 쓰는 대체 키의 중복 횟수. 아래 fallbackExternalId 참고.
    Map<String, Integer> fallbackOccurrences = new HashMap<>();
    for (Object item : txList) {
      if (!(item instanceof Map<?, ?> txMap)) continue;
      String externalId = stringValue(txMap.get("id"));
      if (externalId == null || externalId.isBlank()) {
        externalId = fallbackExternalId(fintechUseNum, txMap, fallbackOccurrences);
      }
      if (transactionRepository.findByUserIdAndExternalTxId(numericUserId, externalId).isPresent())
        continue;

      String type = stringValue(txMap.get("type"));
      String direction =
          "WITHDRAWAL".equalsIgnoreCase(type) || "OUT".equalsIgnoreCase(type) ? "OUT" : "IN";
      long amount = longValue(txMap.get("amount"), 0L);
      if (amount == 0) continue;

      TransactionEntity tx = new TransactionEntity();
      tx.setUserId(numericUserId);
      tx.setExternalTxId(externalId);
      tx.setTxnDate(parseDate(stringValue(txMap.get("date"))));
      tx.setAmount(Math.abs(amount));
      tx.setDirection(direction);
      tx.setMerchant(stringValue(txMap.get("merchant")));
      tx.setMemo(stringValue(txMap.get("memo")));
      tx.setSource("OPEN_BANKING");
      categorizer.categorize(tx);
      transactionRepository.save(tx);
      newTransactions.add(tx);
      saved++;
    }
    // 핀테크이용번호는 계좌를 식별하는 값이므로 뒤 4자리만 남긴다.
    log.info(LogMessages.Transaction.SYNC, userId, PiiMasker.account(fintechUseNum), saved);

    if (saved > 0) {
      // 신규 거래가 들어왔으면 실시간 알림 + 예산 재평가를 같은 트랜잭션에서 아웃박스로 예약한다.
      // 예전에는 하루 1회 배치 동기화가 전부라 "방금 결제" 를 사용자가 알 방법이 없었다.
      publishTransactionEvents(userId, newTransactions);
      budgetService.evaluateAndNotify(userId);
    }
    return saved;
  }

  @Override
  public List<TransactionEntity> recent(String userId, LocalDate from, LocalDate to) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) return List.of();
    if (from == null && to == null)
      return transactionRepository.findByUserIdOrderByTxnDateDesc(numericUserId);
    LocalDate effectiveFrom = from == null ? LocalDate.now().minusMonths(3) : from;
    LocalDate effectiveTo = to == null ? LocalDate.now() : to;
    return transactionRepository.findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(
        numericUserId, effectiveFrom, effectiveTo);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TransactionEntity> recent(
      String userId, LocalDate from, LocalDate to, Pageable pageable) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) {
      return Page.empty(pageable);
    }
    if (from == null && to == null) {
      return transactionRepository.findByUserId(numericUserId, pageable);
    }
    LocalDate effectiveFrom = from == null ? LocalDate.now().minusMonths(3) : from;
    LocalDate effectiveTo = to == null ? LocalDate.now() : to;
    return transactionRepository.findByUserIdAndTxnDateBetween(
        numericUserId, effectiveFrom, effectiveTo, pageable);
  }

  @Override
  @Transactional
  public int recategorizeAll(String userId) {
    Long numericUserId = parseUserId(userId);
    if (numericUserId == null) return 0;
    List<TransactionEntity> all =
        transactionRepository.findByUserIdOrderByTxnDateDesc(numericUserId);
    List<TransactionEntity> changed = new ArrayList<>();
    for (TransactionEntity tx : all) {
      String prevExpense = tx.getExpenseCategoryCode();
      String prevIncome = tx.getIncomeCategoryCode();
      tx.setExpenseCategoryCode(null);
      tx.setIncomeCategoryCode(null);
      categorizer.categorize(tx);
      if (!eq(prevExpense, tx.getExpenseCategoryCode())
          || !eq(prevIncome, tx.getIncomeCategoryCode())) {
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
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        return fallback;
      }
    }
    return fallback;
  }

  /**
   * 기관이 거래 ID 를 주지 않을 때 쓰는 대체 키.
   *
   * <p>예전에는 {@code 계좌-날짜-금액} 이었다. 같은 날 같은 금액의 서로 다른 거래(예: 4,500원 커피를 두 번)가 같은 키가 되어 <b>두 번째가 중복으로
   * 판정돼 조용히 사라졌다.</b> 자산 관리 앱에서 거래 누락은 예산·지출 집계를 그대로 틀리게 만든다.
   *
   * <p>가맹점·메모·구분까지 넣어 구별력을 높이고, 그래도 같은 것이 여러 건이면 배치 안에서의 등장 순서를 붙인다. 같은 응답을 다시 동기화해도 같은 키가 나오므로 중복
   * 저장되지 않는다.
   */
  private String fallbackExternalId(
      String fintechUseNum, Map<?, ?> txMap, Map<String, Integer> occurrences) {
    String base =
        String.join(
            "|",
            fintechUseNum,
            String.valueOf(stringValue(txMap.get("date"))),
            String.valueOf(stringValue(txMap.get("amount"))),
            String.valueOf(stringValue(txMap.get("type"))),
            String.valueOf(stringValue(txMap.get("merchant"))),
            String.valueOf(stringValue(txMap.get("memo"))));
    int seq = occurrences.merge(base, 1, Integer::sum);
    return seq == 1 ? base : base + "#" + seq;
  }

  /**
   * 거래일 파싱.
   *
   * <p>해석하지 못하면 오늘로 둔다. 다만 <b>조용히 넘기지 않는다.</b> 거래일이 틀리면 그 거래가 다른 예산 기간과 다른 월 리포트에 들어가는데, 로그가 없으면
   * 아무도 알 수 없다.
   */
  private LocalDate parseDate(String s) {
    if (s == null || s.isBlank()) {
      log.warn("거래일이 비어 있어 오늘로 기록한다 — 집계가 어긋날 수 있다");
      return LocalDate.now();
    }
    try {
      return LocalDate.parse(s);
    } catch (Exception e) {
      try {
        return LocalDate.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
      } catch (Exception ex) {
        log.warn("거래일을 해석하지 못해 오늘로 기록한다 raw={} — 집계가 어긋날 수 있다", s);
        return LocalDate.now();
      }
    }
  }

  private Long parseUserId(String userId) {
    if (userId == null || userId.isBlank()) return null;
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
