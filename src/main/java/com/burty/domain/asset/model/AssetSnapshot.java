/**
 *
 *
 * <pre>
 * <b>Description  : 자산 도메인 모델 (AssetSnapshot)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.asset.model
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
package com.burty.domain.asset.model;

public record AssetSnapshot(double totalAsset, double monthlySpend, double volatilityPercent) {}
