package com.burty.adapter.in.web.dto;

public class LogoutResponse {
    private boolean logout;

    public LogoutResponse() {}

    public LogoutResponse(boolean logout) {
        this.logout = logout;
    }

    public boolean isLogout() { return logout; }
    public void setLogout(boolean logout) { this.logout = logout; }
}
