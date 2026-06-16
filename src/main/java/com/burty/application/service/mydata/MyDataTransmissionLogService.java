package com.burty.application.service.mydata;

import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity;
import com.burty.domain.mydata.entity.MyDataTransmissionLogEntity.Direction;
import com.burty.domain.mydata.repository.MyDataTransmissionLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyDataTransmissionLogService {

  private final MyDataTransmissionLogRepository logRepository;

  @Transactional
  public void logOutbound(String userId, String institutionCode, String action, String summary) {
    persist(userId, institutionCode, action, Direction.OUTBOUND, summary);
  }

  @Transactional
  public void logInbound(String userId, String institutionCode, String action, String summary) {
    persist(userId, institutionCode, action, Direction.INBOUND, summary);
  }

  public List<MyDataTransmissionLogEntity> listForUser(String userId, int days) {
    LocalDateTime since = LocalDateTime.now().minusDays(Math.max(1, days));
    return logRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since);
  }

  public List<MyDataTransmissionLogEntity> exportSince(LocalDateTime since) {
    return logRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
  }

  private void persist(
      String userId, String institutionCode, String action, Direction direction, String summary) {
    MyDataTransmissionLogEntity entity = new MyDataTransmissionLogEntity();
    entity.setUserId(userId);
    entity.setInstitutionCode(institutionCode);
    entity.setAction(action);
    entity.setDirection(direction);
    entity.setSummary(summary);
    entity.setCreatedAt(LocalDateTime.now());
    logRepository.save(entity);
  }
}
