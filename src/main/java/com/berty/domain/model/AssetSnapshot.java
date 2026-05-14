package com.berty.domain.model;

public class AssetSnapshot {
    private double totalAsset;
    private double monthlySpend;
    private double volatilityPercent;

    public AssetSnapshot(double totalAsset, double monthlySpend, double volatilityPercent) {
        this.totalAsset = totalAsset;
        this.monthlySpend = monthlySpend;
        this.volatilityPercent = volatilityPercent;
    }

    public double getTotalAsset() { return totalAsset; }
    public double getMonthlySpend() { return monthlySpend; }
    public double getVolatilityPercent() { return volatilityPercent; }
}
