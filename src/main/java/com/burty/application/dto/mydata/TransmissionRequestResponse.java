package com.burty.application.dto.mydata;

import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import java.time.LocalDateTime;

public record TransmissionRequestResponse(
    Long requestId,
    String userId,
    String institutionCode,
    String scope,
    String status,
    LocalDateTime requestedAt,
    LocalDateTime authorizedAt,
    LocalDateTime revokedAt,
    LocalDateTime consentExpiresAt) {

  public static TransmissionRequestResponse from(MyDataTransmissionRequestEntity entity) {
    return new TransmissionRequestResponse(
        entity.getRequestId(),
        entity.getUserId(),
        entity.getInstitutionCode(),
        entity.getScope(),
        entity.getStatus().name(),
        entity.getRequestedAt(),
        entity.getAuthorizedAt(),
        entity.getRevokedAt(),
        entity.getConsentExpiresAt());
  }
}
