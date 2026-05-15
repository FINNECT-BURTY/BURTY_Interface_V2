package com.burty.core.dto.request;

public class ApiRequest<T> {

    private T payload;

    public ApiRequest() {}

    public ApiRequest(T payload) {
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }
    public void setPayload(T payload) {
        this.payload = payload;
    }
}
