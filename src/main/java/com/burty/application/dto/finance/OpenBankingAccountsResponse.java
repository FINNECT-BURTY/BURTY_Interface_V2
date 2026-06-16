/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (OpenBankingAccountsResponse)</b>
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

public record OpenBankingAccountsResponse(
    String provider, String userId, List<OpenBankingAccountItemResponse> accounts) {

  @SuppressWarnings("unchecked")
  public static OpenBankingAccountsResponse fromMap(Map<String, Object> map) {
    Object accountsRaw = map.get("accounts");
    List<OpenBankingAccountItemResponse> accounts = List.of();
    if (accountsRaw instanceof List<?> list) {
      accounts =
          list.stream().map(OpenBankingAccountItemResponse::fromMap).collect(Collectors.toList());
    }
    Object provider = map.get("provider");
    Object userId = map.get("userId");
    return new OpenBankingAccountsResponse(
        provider == null ? null : String.valueOf(provider),
        userId == null ? null : String.valueOf(userId),
        accounts);
  }
}
