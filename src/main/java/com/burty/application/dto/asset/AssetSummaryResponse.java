/**
 *
 *
 * <pre>
 * <b>Description  : 자산 응답 DTO (AssetSummaryResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.asset
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
package com.burty.application.dto.asset;

public record AssetSummaryResponse(
    String userId,
    double totalAsset,
    double monthlySpend,
    double volatilityPercent,
    // 0 이면 금액은 "0원" 이 아니라 "모름" 이다. 화면은 이 값으로 빈 상태를 가른다.
    int linkedInstitutionCount,
    // 조회에 실패해 합계에서 빠진 기관 수. 0 보다 크면 합계가 실제보다 작다.
    int failedInstitutionCount) {}
