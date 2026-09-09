package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.burty.application.service.batch.PersonalDataPurger;
import com.burty.application.service.batch.PersonalDataRetentionBatch;
import com.burty.application.service.support.AuditLogger;
import com.burty.domain.admin.model.AuditEvent;
import com.burty.domain.user.entity.DataErasureRequestEntity;
import com.burty.domain.user.entity.DataErasureRequestEntity.Status;
import com.burty.domain.user.repository.DataErasureRequestRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 보존기간 만료 배치가 대상을 고르고 실패를 다루는 방식을 못 박는다.
 *
 * <p>가장 중요한 것은 <b>한 건의 실패가 나머지를 막지 않는 것</b>이다. 배치가 첫 건에서 멈추면 뒤의 사용자들은 파기되지 않은 채 다음 날까지 남는데, 무인 실행이라
 * 아무도 모른다.
 */
class PersonalDataRetentionBatchTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 3, 0);

  private DataErasureRequestRepository erasures;
  private PersonalDataPurger purger;
  private List<AuditEvent> audited;
  private PersonalDataRetentionBatch batch;

  @BeforeEach
  void setUp() {
    erasures = mock(DataErasureRequestRepository.class);
    purger = mock(PersonalDataPurger.class);
    audited = new ArrayList<>();

    AuditLogger auditLogger =
        new AuditLogger(audited::add, new SingletonProvider<>(new SimpleMeterRegistry()));
    Clock clock =
        Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    batch = new PersonalDataRetentionBatch(erasures, purger, auditLogger, clock);
  }

  private static DataErasureRequestEntity request(Long erasureId, Long userId) {
    DataErasureRequestEntity request = new DataErasureRequestEntity();
    request.setErasureId(erasureId);
    request.setUserId(userId);
    request.setStatus(Status.IMMEDIATE_DONE);
    return request;
  }

  private void due(DataErasureRequestEntity... requests) {
    when(erasures.findByStatusAndRetentionUntilLessThanEqualOrderByErasureIdAsc(
            any(Status.class), any(LocalDateTime.class)))
        .thenReturn(List.of(requests));
  }

  @Test
  @DisplayName("한 건이 실패해도 나머지는 파기한다")
  void oneFailureDoesNotStopTheRest() {
    // 배치가 첫 건에서 멈추면 뒤 사용자들은 파기되지 않은 채 남는다. 무인 실행이라
    // 다음 날까지 아무도 모른다.
    due(request(1L, 100L), request(2L, 200L), request(3L, 300L));
    when(purger.purge(eq(1L), any())).thenThrow(new IllegalStateException("일부러 실패"));
    when(purger.purge(eq(2L), any())).thenReturn("ok-2");
    when(purger.purge(eq(3L), any())).thenReturn("ok-3");

    batch.purgeExpiredRetention();

    verify(purger).purge(eq(2L), any());
    verify(purger).purge(eq(3L), any());
  }

  @Test
  @DisplayName("실패한 건은 감사 기록을 남기지 않는다")
  void failedPurgeIsNotAudited() {
    // 파기되지 않았는데 완료로 기록되면 증빙이 사실과 달라진다.
    due(request(1L, 100L), request(2L, 200L));
    when(purger.purge(eq(1L), any())).thenThrow(new IllegalStateException("일부러 실패"));
    when(purger.purge(eq(2L), any())).thenReturn("ok-2");

    batch.purgeExpiredRetention();

    assertEquals(1, audited.size());
    assertTrue(audited.get(0).target().contains("200"), audited.get(0).target());
  }

  @Test
  @DisplayName("대상이 없으면 아무것도 하지 않는다")
  void noDueRequestsDoesNothing() {
    due();

    batch.purgeExpiredRetention();

    verify(purger, never()).purge(anyLong(), any());
    assertTrue(audited.isEmpty());
  }

  @Test
  @DisplayName("즉시 파기가 끝난 건만 대상으로 고른다")
  void selectsOnlyImmediateDoneRequests() {
    // 아직 즉시 파기도 안 끝난 건을 보존 만료로 처리하면 순서가 뒤집힌다.
    due(request(1L, 100L));

    batch.purgeExpiredRetention();

    verify(erasures)
        .findByStatusAndRetentionUntilLessThanEqualOrderByErasureIdAsc(
            eq(Status.IMMEDIATE_DONE), any(LocalDateTime.class));
  }

  @Test
  @DisplayName("파기에는 배치가 잡은 시각을 그대로 넘긴다")
  void passesBatchClockToPurger() {
    // 건마다 now() 를 다시 부르면 자정을 넘길 때 같은 배치 안에서 기준 시각이 갈라진다.
    due(request(1L, 100L));

    batch.purgeExpiredRetention();

    verify(purger).purge(eq(1L), eq(NOW));
  }

  private record SingletonProvider<T>(T instance) implements ObjectProvider<T> {
    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }
  }
}
