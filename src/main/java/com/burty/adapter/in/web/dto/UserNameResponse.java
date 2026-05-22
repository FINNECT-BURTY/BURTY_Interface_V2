package com.burty.adapter.in.web.dto;

public class UserNameResponse {
    private String name;

    public UserNameResponse() {}

    public UserNameResponse(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}