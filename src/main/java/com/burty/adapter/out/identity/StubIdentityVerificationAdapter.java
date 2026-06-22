package com.burty.adapter.out.identity;

import com.burty.application.port.out.identity.IdentityVerificationPort;
import com.burty.core.constant.AppMessages;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** NICE/KCB/Pass 연동 전 개발용 본인확인 스텁. */
@Component
@ConditionalOnProperty(
    prefix = "burty.identity",
    name = "stub-mode",
    havingValue = "true",
    matchIfMissing = true)
public class StubIdentityVerificationAdapter implements IdentityVerificationPort {

  @Override
  public IdentityVerificationResult verify(IdentityVerificationRequest request) {
    if (request.name() == null
        || request.name().isBlank()
        || request.phone() == null
        || request.phone().isBlank()) {
      return new IdentityVerificationResult(false, null, null, AppMessages.Identity.VERIFY_FAILED);
    }
    String ci = "CI-" + UUID.nameUUIDFromBytes((request.phone() + request.name()).getBytes());
    String di = "DI-" + UUID.nameUUIDFromBytes(request.phone().getBytes());
    return new IdentityVerificationResult(
        true, ci, di.toString(), AppMessages.Identity.VERIFY_SUCCESS);
  }
}
