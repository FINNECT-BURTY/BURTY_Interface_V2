package com.burty.adapter.in.web.dto;

public class AdminRegisterRequest {
    private String setupKey;
    private String username;
    private String password;
    private String role;

    public String getSetupKey() { return setupKey; }
    public void setSetupKey(String setupKey) { this.setupKey = setupKey; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
