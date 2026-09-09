package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.application.service.batch.PersonalDataPurger;
import com.burty.domain.finance.entity.TransferOrderEntity;
import com.burty.domain.finance.repository.TransferOrderRepository;
import com.burty.domain.notification.entity.NotificationEntity;
import com.burty.domain.notification.repository.NotificationRepository;
import com.burty.domain.transaction.entity.TransactionEntity;
import com.burty.domain.transaction.repository.TransactionRepository;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.DataErasureRequestEntity.Status;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 보존기간 만료 파기의 불변식을 못 박는다.
 *
 * <p>배치는 무인 실행이라 잘못 돌아도 아무도 모른다. 개인정보 파기는 특히 되돌릴 수 없다 — 지우지 않으면 개인정보보호법 위반이고, 두 번 돌아 엉뚱한 것을 지우면 복구할
 * 방법이 없다.
 *
 * <p>배치 패키지 열두 개 중 열한 개에 테스트가 없었다.
 */
class PersonalDataPurgeTests {

  private static final Long ERASURE_ID = 10L;
  private static final Long USER_ID = 7L;

  private DataErasureRequestRepository erasures;
  private TransactionRepository transactions;
  private TransferOrderRepository orders;
  private NotificationRepository notifications;
  private PersonalDataPurger purger;

  @BeforeEach
  void setUp() {
    erasures = mock(DataErasureRequestRepository.class);
    transactions = mock(TransactionRepository.class);
    orders = mock(TransferOrderRepository.class);
    notifications = mock(NotificationRepository.class);

    when(transactions.findByUserIdOrderByTxnDateDesc(anyLong()))
        .thenReturn(List.of(new TransactionEntity(), new TransactionEntity()));
    when(orders.findByUser_UserId(anyLong())).thenReturn(List.of(new TransferOrderEntity()));
    when(notifications.findByRecipientUser_UserIdOrderByNotificationIdDesc(anyLong()))
        .thenReturn(List.of(new NotificationEntity()));

    purger = new PersonalDataPurger(erasures, transactions, orders, notifications);
  }

  private DataErasureRequestEntity request(Status status) {
    DataErasureRequestEntity request = new DataErasureRequestEntity();
    request.setErasureId(ERASURE_ID);
    request.setUserId(USER_ID);
    request.setStatus(status);
    request.setSummary("즉시 파기 완료");
    when(erasures.findById(ERASURE_ID)).thenReturn(Optional.of(request));
    return request;
  }

  @Test
  @DisplayName("거래·이체·알림을 지우고 파기 완료로 표시한다")
  void purgesResidualRecords() {
    DataErasureRequestEntity target = request(Status.IMMEDIATE_DONE);

    String summary = purger.purge(ERASURE_ID, LocalDateTime.now());

    verify(transactions).deleteAll(any());
    verify(orders).deleteAll(any());
    verify(notifications).deleteAll(any());
    assertEquals(Status.FULLY_PURGED, target.getStatus());
    assertNotNull(target.getPurgedAt());
    assertTrue(summary.contains("transactions=2"), summary);
  }

  @Test
  @DisplayName("이미 파기된 건은 두 번 지우지 않는다")
  void alreadyPurgedIsIdempotent() {
    // 배치가 겹쳐 돌거나 재시도될 수 있다. 두 번째 실행이 다시 삭제를 시도하면
    // 그 사이 새로 쌓인 기록까지 지운다.
    request(Status.FULLY_PURGED);

    purger.purge(ERASURE_ID, LocalDateTime.now());

    verify(transactions, never()).deleteAll(any());
    verify(orders, never()).deleteAll(any());
    verify(notifications, never()).deleteAll(any());
  }

  @Test
  @DisplayName("파기 요약을 덮어쓰지 않고 이어 붙인다")
  void summaryIsAppendedNotReplaced() {
    // 요약은 증빙 자료다. 즉시 파기 내역을 덮어쓰면 무엇을 언제 지웠는지 남지 않는다.
    DataErasureRequestEntity target = request(Status.IMMEDIATE_DONE);

    purger.purge(ERASURE_ID, LocalDateTime.now());

    assertTrue(target.getSummary().startsWith("즉시 파기 완료"), target.getSummary());
    assertTrue(target.getSummary().contains("retentionPurge="), target.getSummary());
  }

  @Test
  @DisplayName("없는 파기 요청은 조용히 넘어가지 않는다")
  void missingRequestThrows() {
    // 조용히 성공으로 처리하면 파기되지 않은 건이 완료로 기록된다.
    when(erasures.findById(anyLong())).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> purger.purge(999L, LocalDateTime.now()));
  }

  @Test
  @DisplayName("파기 시각은 호출자가 준 시각을 쓴다")
  void purgedAtUsesGivenClock() {
    // 배치가 자정을 넘겨 돌면 대상 선정 시각과 기록 시각이 갈라진다. 같은 시각을
    // 써야 "언제 기준으로 만료된 건인지" 가 증빙으로 남는다.
    DataErasureRequestEntity target = request(Status.IMMEDIATE_DONE);
    LocalDateTime given = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

    purger.purge(ERASURE_ID, given);

    assertEquals(given, target.getPurgedAt());
  }
}
