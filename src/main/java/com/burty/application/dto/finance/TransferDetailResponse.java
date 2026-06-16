/**
 *
 *
 * <pre>
 * <b>Description  : 금융 응답 DTO (TransferDetailResponse)</b>
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

public record TransferDetailResponse(
    boolean found, String transferId, String status, Boolean familyNotified) {
  public static TransferDetailResponse notFound(String transferId) {
    return new TransferDetailResponse(false, transferId, null, null);
  }

  public static TransferDetailResponse found(
      String transferId, String status, boolean familyNotified) {
    return new TransferDetailResponse(true, transferId, status, familyNotified);
  }
}
