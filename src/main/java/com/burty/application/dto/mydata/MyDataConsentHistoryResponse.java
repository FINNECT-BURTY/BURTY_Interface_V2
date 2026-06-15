package com.burty.application.dto.mydata;

import com.burty.domain.mydata.entity.MyDataConsentHistoryEntity;
import java.time.LocalDateTime;

public record MyDataConsentHistoryResponse(
    Long consentHistoryId,
    Long transmissionRequestId,
    String userId,
    String institutionCode,
    String scope,
    String consentVersion,
    LocalDateTime agreedAt,
    LocalDateTime revokedAt,
    String revokeReason) {

  public static MyDataConsentHistoryResponse from(MyDataConsentHistoryEntity entity) {
    return new MyDataConsentHistoryResponse(
        entity.getConsentHistoryId(),
        entity.getTransmissionRequestId(),
        entity.getUserId(),
        entity.getInstitutionCode(),
        entity.getScope(),
        entity.getConsentVersion(),
        entity.getAgreedAt(),
        entity.getRevokedAt(),
        entity.getRevokeReason());
  }
}
