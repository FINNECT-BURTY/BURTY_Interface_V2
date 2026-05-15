package com.burty.adapter.out.mydata.dto;

public class MyDataAssetResponse {
    private Double totalAsset;
    private Double monthlySpend;
    private Double volatilityPercent;

    public Double getTotalAsset() { return totalAsset; }
    public void setTotalAsset(Double totalAsset) { this.totalAsset = totalAsset; }
    public Double getMonthlySpend() { return monthlySpend; }
    public void setMonthlySpend(Double monthlySpend) { this.monthlySpend = monthlySpend; }
    public Double getVolatilityPercent() { return volatilityPercent; }
    public void setVolatilityPercent(Double volatilityPercent) { this.volatilityPercent = volatilityPercent; }
}
