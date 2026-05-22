package com.burty.application.port.in;

public interface AdminAuthUseCase {

    String login(String username, String password);

    void register(String setupKey, String username, String password, String role);
}
