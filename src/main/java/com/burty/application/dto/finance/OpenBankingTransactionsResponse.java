/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (OpenBankingTransactionsResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.finance
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
package com.burty.application.dto.finance;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OpenBankingTransactionsResponse(
    String provider,
    String userId,
    String fintechUseNum,
    List<OpenBankingTransactionItemResponse> transactions) {

  @SuppressWarnings("unchecked")
  public static OpenBankingTransactionsResponse fromMap(Map<String, Object> map) {
    Object transactionsRaw = map.get("transactions");
    List<OpenBankingTransactionItemResponse> transactions = List.of();
    if (transactionsRaw instanceof List<?> list) {
      transactions =
          list.stream()
              .map(OpenBankingTransactionItemResponse::fromMap)
              .collect(Collectors.toList());
    }
    return new OpenBankingTransactionsResponse(
        stringVal(map, "provider"),
        stringVal(map, "userId"),
        stringVal(map, "fintechUseNum"),
        transactions);
  }

  private static String stringVal(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? null : String.valueOf(value);
  }
}
