package com.burty.application.dto.identity;

public record IdentityVerificationRequest(
    String userId, String name, String phone, String birthDate, String carrier) {}
