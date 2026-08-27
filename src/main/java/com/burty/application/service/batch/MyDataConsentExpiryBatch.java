package com.burty.application.service.batch;

import com.burty.application.service.mydata.MyDataConsentExpiryProcessor;
import com.burty.domain.mydata.entity.MyDataTransmissionRequestEntity;
import com.burty.domain.mydata.repository.MyDataTransmissionRequestRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 마이데이터 전송요구 동의 만료 처리.
 *
 * <p>동의에는 유효기간이 있고 ({@code consent_expires_at}), 만료되면 그 시점부터 수집이 중단되고 보유 데이터도 정리되어야 한다. 기존에는 만료 컬럼만
 * 있고 <b>아무도 그것을 보지 않았다.</b> 한 번 동의하면 기관 토큰이 영구히 살아 있는 상태였다.
 *
 * <p>만료 임박 건은 미리 알려 재동의 기회를 준다. 예고 없이 끊기면 사용자 입장에서는 서비스가 고장난 것으로 보인다.
 */
@Service
public class MyDataConsentExpiryBatch {

  private static final Logger log = LoggerFactory.getLogger(MyDataConsentExpiryBatch.class);

  private final MyDataTransmissionRequestRepository requestRepository;
  private final MyDataConsentExpiryProcessor processor;
  private final Clock clock;
  private final ObjectProvider<MeterRegistry> meterRegistry;
  private final int warnAheadDays;

  public MyDataConsentExpiryBatch(
      MyDataTransmissionRequestRepository requestRepository,
      MyDataConsentExpiryProcessor processor,
      Clock clock,
      ObjectProvider<MeterRegistry> meterRegistry,
      @Value("${burty.mydata.consent-warn-ahead-days:7}") int warnAheadDays) {
    this.requestRepository = requestRepository;
    this.processor = processor;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
    this.warnAheadDays = warnAheadDays;
  }

  /** 스케줄 진입점. 락·스케줄링만 담당하고 처리는 {@link #runOnce()} 에 위임한다 (테스트 가능성). */
  @Scheduled(cron = "${burty.mydata.consent-expiry-cron:0 10 * * * *}")
  @SchedulerLock(name = "myDataConsentExpiry", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
  public void run() {
    runOnce();
  }

  public void runOnce() {
    LocalDateTime now = LocalDateTime.now(clock);
    expireLapsed(now);
    warnExpiringSoon(now);
  }

  private void expireLapsed(LocalDateTime now) {
    List<MyDataTransmissionRequestEntity> expired = requestRepository.findExpirable(now);
    if (expired.isEmpty()) {
      return;
    }
    log.info("마이데이터 동의 만료 처리 시작 — 대상 {}건", expired.size());
    int processed = 0;
    for (MyDataTransmissionRequestEntity request : expired) {
      try {
        if (processor.expire(request.getRequestId(), now)) {
          processed++;
        }
      } catch (RuntimeException e) {
        // 한 건 실패가 나머지를 막지 않게 한다. 다음 주기에 다시 시도된다.
        log.error(
            "동의 만료 처리 실패 requestId={} userId={} reason={}",
            request.getRequestId(),
            request.getUserId(),
            e.getMessage(),
            e);
      }
    }
    count("expired", processed);
  }

  private void warnExpiringSoon(LocalDateTime now) {
    List<MyDataTransmissionRequestEntity> soon =
        requestRepository.findExpiringSoon(now, now.plusDays(warnAheadDays));
    for (MyDataTransmissionRequestEntity request : soon) {
      try {
        processor.warnExpiringSoon(request.getRequestId());
      } catch (RuntimeException e) {
        log.warn("동의 만료 예고 알림 실패 requestId={} reason={}", request.getRequestId(), e.getMessage());
      }
    }
    count("warned", soon.size());
  }

  private void count(String outcome, int amount) {
    if (amount <= 0) {
      return;
    }
    MeterRegistry registry = meterRegistry.getIfAvailable();
    if (registry != null) {
      registry.counter("burty.mydata.consent", "outcome", outcome).increment(amount);
    }
  }
}
