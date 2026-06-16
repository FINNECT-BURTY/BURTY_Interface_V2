/**
 *
 *
 * <pre>
 * <b>Description  : 마이데이터 응답 DTO (MyDataAssetResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.mydata.dto
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
package com.burty.adapter.out.mydata.dto;

public class MyDataAssetResponse {
  private Double totalAsset;
  private Double monthlySpend;
  private Double volatilityPercent;

  public Double getTotalAsset() {
    return totalAsset;
  }

  public void setTotalAsset(Double totalAsset) {
    this.totalAsset = totalAsset;
  }

  public Double getMonthlySpend() {
    return monthlySpend;
  }

  public void setMonthlySpend(Double monthlySpend) {
    this.monthlySpend = monthlySpend;
  }

  public Double getVolatilityPercent() {
    return volatilityPercent;
  }

  public void setVolatilityPercent(Double volatilityPercent) {
    this.volatilityPercent = volatilityPercent;
  }
}
