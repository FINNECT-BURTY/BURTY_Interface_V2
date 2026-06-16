/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (OpenBankingTransactionItemResponse)</b>
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

import java.util.Map;

public record OpenBankingTransactionItemResponse(String type, long amount, String memo) {

  @SuppressWarnings("unchecked")
  public static OpenBankingTransactionItemResponse fromMap(Object raw) {
    if (!(raw instanceof Map<?, ?> map)) {
      return new OpenBankingTransactionItemResponse(null, 0L, null);
    }
    return new OpenBankingTransactionItemResponse(
        stringVal((Map<String, Object>) map, "type"),
        longVal((Map<String, Object>) map, "amount"),
        stringVal((Map<String, Object>) map, "memo"));
  }

  private static String stringVal(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private static long longVal(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    return 0L;
  }
}
