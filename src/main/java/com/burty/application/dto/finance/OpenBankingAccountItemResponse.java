/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (OpenBankingAccountItemResponse)</b>
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

public record OpenBankingAccountItemResponse(
    String fintechUseNum, String bankName, String accountMasked) {

  @SuppressWarnings("unchecked")
  public static OpenBankingAccountItemResponse fromMap(Object raw) {
    if (!(raw instanceof Map<?, ?> map)) {
      return new OpenBankingAccountItemResponse(null, null, null);
    }
    return new OpenBankingAccountItemResponse(
        stringVal((Map<String, Object>) map, "fintechUseNum"),
        stringVal((Map<String, Object>) map, "bankName"),
        stringVal((Map<String, Object>) map, "accountMasked"));
  }

  private static String stringVal(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? null : String.valueOf(value);
  }
}
