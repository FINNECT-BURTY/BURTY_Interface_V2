/**
 *
 *
 * <pre>
 * <b>Description  : 자산 응답 DTO (AssetTrendItemResponse)</b>
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

public record AssetTrendItemResponse(String month, long totalAsset) {}
