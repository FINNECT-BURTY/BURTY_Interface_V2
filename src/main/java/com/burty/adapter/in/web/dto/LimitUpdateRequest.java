package com.burty.adapter.in.web.dto;

public class LimitUpdateRequest {
    private String userId;
    private long limit;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }
}
