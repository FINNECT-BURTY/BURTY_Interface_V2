package com.berty.adapter.in.web.dto;

public class TransferRequest {
    private String userId;
    private String fromAccount;
    private String toAccount;
    private long amount;
    private String description;
    private String assertionToken;
    private String idempotencyKey;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssertionToken() { return assertionToken; }
    public void setAssertionToken(String assertionToken) { this.assertionToken = assertionToken; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
