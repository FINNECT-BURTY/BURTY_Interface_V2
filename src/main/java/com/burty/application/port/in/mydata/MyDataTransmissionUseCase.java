package com.burty.application.port.in.mydata;

import com.burty.domain.mydata.entity.MyDataConsentHistoryEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import java.util.List;

public interface MyDataTransmissionUseCase {

  MyDataTransmissionRequestEntity createRequest(
      String userId, String institutionCode, String scope);

  List<MyDataTransmissionRequestEntity> listRequests(String userId);

  boolean revokeRequest(String userId, Long requestId, String reason);

  List<MyDataConsentHistoryEntity> listConsents(String userId);

  List<MyDataTransmissionLogEntity> listTransmissionLogs(String userId, int days);

  int syncAccounts(String userId);
}
