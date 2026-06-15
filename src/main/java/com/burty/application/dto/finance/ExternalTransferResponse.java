/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (ExternalTransferResponse)</b>
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

public record ExternalTransferResponse(
    String provider,
    String transactionId,
    String userId,
    String toAccount,
    long amount,
    String status) {

  public static ExternalTransferResponse fromMap(Map<String, Object> map) {
    return new ExternalTransferResponse(
        stringVal(map, "provider"),
        stringVal(map, "transactionId"),
        stringVal(map, "userId"),
        stringVal(map, "toAccount"),
        longVal(map, "amount"),
        stringVal(map, "status"));
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
    if (value instanceof String text) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException ignored) {
        return 0L;
      }
    }
    return 0L;
  }
}
