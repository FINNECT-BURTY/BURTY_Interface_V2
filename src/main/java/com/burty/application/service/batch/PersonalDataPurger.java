package com.burty.application.service.batch;

import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.notification.repository.NotificationRepository;
import com.burty.domain.transaction.repository.TransactionRepository;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.DataErasureRequestEntity.Status;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보존기간이 끝난 잔여 개인정보의 실제 파기.
 *
 * <p>배치 클래스와 분리한 이유는 트랜잭션 경계 때문이다. 한 사용자의 파기는 하나의 트랜잭션으로 끝나야 하고, 한 건이 실패해도 다른 사용자의 파기는 계속돼야 한다. 배치
 * 안의 메서드를 자기 호출하면 {@code @Transactional} 프록시가 적용되지 않는다.
 *
 * <p><b>감사 로그는 지우지 않는다.</b> 감사 로그에는 이미 직접 식별정보가 없고 (익명화된 user_id 만 남는다), 해시 체인이 걸려 있어 중간 행을 지우면 무결성
 * 검증이 깨진다. 파기 대상은 거래·알림 기록이다.
 */
@Component
public class PersonalDataPurger {

  private static final Logger log = LoggerFactory.getLogger(PersonalDataPurger.class);

  private final DataErasureRequestRepository erasureRepository;
  private final TransactionRepository transactionRepository;
  private final TransferOrderRepository transferOrderRepository;
  private final NotificationRepository notificationRepository;

  public PersonalDataPurger(
      DataErasureRequestRepository erasureRepository,
      TransactionRepository transactionRepository,
      TransferOrderRepository transferOrderRepository,
      NotificationRepository notificationRepository) {
    this.erasureRepository = erasureRepository;
    this.transactionRepository = transactionRepository;
    this.transferOrderRepository = transferOrderRepository;
    this.notificationRepository = notificationRepository;
  }

  /**
   * 한 건의 파기 요청을 처리한다.
   *
   * @return 파기 내역 요약 (증빙용)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String purge(Long erasureId, LocalDateTime now) {
    DataErasureRequestEntity request =
        erasureRepository
            .findById(erasureId)
            .orElseThrow(() -> new IllegalStateException("파기 요청을 찾을 수 없습니다 id=" + erasureId));
    if (request.getStatus() == Status.FULLY_PURGED) {
      return request.getSummary(); // 멱등
    }

    Long userId = request.getUserId();
    Map<String, Integer> summary = new LinkedHashMap<>();

    var transactions = transactionRepository.findByUserIdOrderByTxnDateDesc(userId);
    transactionRepository.deleteAll(transactions);
    summary.put("transactions", transactions.size());

    var orders = transferOrderRepository.findByUser_UserId(userId);
    transferOrderRepository.deleteAll(orders);
    summary.put("transferOrders", orders.size());

    var notifications =
        notificationRepository.findByRecipientUser_UserIdOrderByNotificationIdDesc(userId);
    notificationRepository.deleteAll(notifications);
    summary.put("notifications", notifications.size());

    request.setStatus(Status.FULLY_PURGED);
    request.setPurgedAt(now);
    request.setSummary(request.getSummary() + " | retentionPurge=" + summary);
    erasureRepository.save(request);

    log.info("보존기간 만료 파기 완료 userId={} summary={}", userId, summary);
    return summary.toString();
  }
}
