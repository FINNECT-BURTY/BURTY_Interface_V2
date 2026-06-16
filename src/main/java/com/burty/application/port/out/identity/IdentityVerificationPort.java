package com.burty.application.port.out.identity;

/** 본인확인(CI/DI) 외부 연동 포트. */
public interface IdentityVerificationPort {

  IdentityVerificationResult verify(IdentityVerificationRequest request);

  record IdentityVerificationRequest(String name, String phone, String birthDate, String carrier) {}

  record IdentityVerificationResult(boolean verified, String ci, String di, String message) {}
}
