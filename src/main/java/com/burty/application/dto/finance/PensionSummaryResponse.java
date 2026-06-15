/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (PensionSummaryResponse)</b>
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

public record PensionSummaryResponse(
    String provider, String userId, long totalBalance, long monthlyPayoutEstimate) {

  public static PensionSummaryResponse fromMap(Map<String, Object> map) {
    return new PensionSummaryResponse(
        stringVal(map, "provider"),
        stringVal(map, "userId"),
        longVal(map, "totalBalance"),
        longVal(map, "monthlyPayoutEstimate"));
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
