/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 애플리케이션 서비스 (RecurringExpenseDetectionService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.action
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
package com.burty.application.service.action;

import com.burty.application.port.out.bank.OpenBankingPort;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.cashflow.model.RecurringExpense;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringExpenseDetectionService {

  private final OpenBankingPort openBankingPort;
  private final MyDataPort myDataPort;

  public List<RecurringExpense> detect(String userId, String fintechUseNum) {
    Map<String, Object> txResponse = openBankingPort.getTransactions(userId, fintechUseNum);
    Object txObj = txResponse.get("transactions");
    Map<String, RecurringExpenseAccumulator> accumulators = new HashMap<>();
    if (txObj instanceof List<?> txList) {
      for (Object item : txList) {
        if (!(item instanceof Map<?, ?> txMap)) {
          continue;
        }
        Object typeObj = txMap.get("type");
        String type = typeObj == null ? "WITHDRAWAL" : String.valueOf(typeObj);
        if (!"WITHDRAWAL".equalsIgnoreCase(type)) {
          continue;
        }
        long amount = extractLong(txMap.get("amount"), 0L);
        Object memoObj = txMap.get("memo");
        String memo = memoObj == null ? "기타" : String.valueOf(memoObj);
        if (amount <= 0) {
          continue;
        }
        RecurringExpenseAccumulator current =
            accumulators.getOrDefault(memo, new RecurringExpenseAccumulator(0L, 0));
        accumulators.put(
            memo,
            new RecurringExpenseAccumulator(current.averageAmount + amount, current.count + 1));
      }
    }

    List<RecurringExpense> detected = new ArrayList<>();
    for (Map.Entry<String, RecurringExpenseAccumulator> entry : accumulators.entrySet()) {
      if (entry.getValue().count >= 1) {
        long avg = Math.max(10_000L, entry.getValue().averageAmount / entry.getValue().count);
        int day = inferDayByMemo(entry.getKey());
        detected.add(new RecurringExpense(entry.getKey(), avg, day));
      }
    }
    if (detected.isEmpty()) {
      AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
      long spend = Math.round(snapshot.monthlySpend());
      detected =
          List.of(
              new RecurringExpense("월세", (long) (spend * 0.32), 25),
              new RecurringExpense("카드값", (long) (spend * 0.25), 15),
              new RecurringExpense("공과금", (long) (spend * 0.10), 21));
    }
    return detected.stream()
        .sorted(Comparator.comparingLong(RecurringExpense::amount).reversed())
        .limit(5)
        .toList();
  }

  private int inferDayByMemo(String memo) {
    String lower = memo.toLowerCase();
    if (lower.contains("rent") || lower.contains("월세")) return 25;
    if (lower.contains("card") || lower.contains("카드")) return 15;
    if (lower.contains("loan") || lower.contains("대출")) return 12;
    return 20;
  }

  private long extractLong(Object value, long defaultValue) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    if (value instanceof String text) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private static class RecurringExpenseAccumulator {
    private final long averageAmount;
    private final int count;

    private RecurringExpenseAccumulator(long averageAmount, int count) {
      this.averageAmount = averageAmount;
      this.count = count;
    }
  }
}
