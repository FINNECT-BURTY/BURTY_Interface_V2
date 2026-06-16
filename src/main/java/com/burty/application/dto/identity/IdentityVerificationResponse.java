package com.burty.application.dto.identity;

public record IdentityVerificationResponse(
    boolean verified, String ci, String di, String message) {}
