package com.burty.application.port.in.identity;

import com.burty.application.port.out.identity.IdentityVerificationPort.IdentityVerificationResult;

public interface IdentityVerificationUseCase {

  IdentityVerificationResult verify(
      String userId, String name, String phone, String birthDate, String carrier);
}
