package com.burty.application.service.identity;

import com.burty.application.port.in.identity.IdentityVerificationUseCase;
import com.burty.application.port.out.identity.IdentityVerificationPort;
import com.burty.application.port.out.identity.IdentityVerificationPort.IdentityVerificationRequest;
import com.burty.application.port.out.identity.IdentityVerificationPort.IdentityVerificationResult;
import com.burty.application.service.support.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService implements IdentityVerificationUseCase {

  private final IdentityVerificationPort identityVerificationPort;
  private final AuditLogger auditLogger;

  @Override
  public IdentityVerificationResult verify(
      String userId, String name, String phone, String birthDate, String carrier) {
    IdentityVerificationResult result =
        identityVerificationPort.verify(
            new IdentityVerificationRequest(name, phone, birthDate, carrier));
    auditLogger.log(
        userId,
        "IDENTITY_VERIFY",
        "CI",
        result.verified() ? "SUCCESS" : "FAILURE",
        result.message());
    return result;
  }
}
