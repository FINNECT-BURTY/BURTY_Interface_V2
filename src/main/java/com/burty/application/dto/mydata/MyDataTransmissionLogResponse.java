package com.burty.application.dto.mydata;

import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity;
import java.time.LocalDateTime;

public record MyDataTransmissionLogResponse(
    Long logId,
    String userId,
    String institutionCode,
    String action,
    String direction,
    String summary,
    LocalDateTime createdAt) {

  public static MyDataTransmissionLogResponse from(MyDataTransmissionLogEntity entity) {
    return new MyDataTransmissionLogResponse(
        entity.getLogId(),
        entity.getUserId(),
        entity.getInstitutionCode(),
        entity.getAction(),
        entity.getDirection().name(),
        entity.getSummary(),
        entity.getCreatedAt());
  }
}
