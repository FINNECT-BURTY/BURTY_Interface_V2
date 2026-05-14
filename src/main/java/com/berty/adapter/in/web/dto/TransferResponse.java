package com.berty.adapter.in.web.dto;

public class TransferResponse {
    private String transferId;
    private String status;
    private boolean familyNotified;

    public TransferResponse() {}
    public TransferResponse(String transferId, String status, boolean familyNotified) {
        this.transferId = transferId;
        this.status = status;
        this.familyNotified = familyNotified;
    }

    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isFamilyNotified() { return familyNotified; }
    public void setFamilyNotified(boolean familyNotified) { this.familyNotified = familyNotified; }
}
