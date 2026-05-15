package com.burty.domain.model;

public class TransferResult {
    private String transferId;
    private String status;
    private boolean familyNotified;

    public TransferResult(String transferId, String status, boolean familyNotified) {
        this.transferId = transferId;
        this.status = status;
        this.familyNotified = familyNotified;
    }

    public String getTransferId() { return transferId; }
    public String getStatus() { return status; }
    public boolean isFamilyNotified() { return familyNotified; }
}
