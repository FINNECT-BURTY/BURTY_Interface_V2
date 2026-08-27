package com.burty.application.service.finance;

import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.entity.DailyTransferUsageId;
import com.burty.domain.finance.repository.DailyTransferUsageRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일일 사용량 행을 만들어 두는 역할만 담당하는 별도 빈.
 *
 * <p>왜 클래스를 나눴는가. 동시 요청이 같은 날짜 행을 만들면 유니크 제약 위반이 나는데, 이걸 <b>호출자와 같은 트랜잭션 안에서</b> try/catch 로 잡아도
 * 소용이 없다. 제약 위반이 발생한 순간 그 트랜잭션은 rollback-only 로 표시되고, 이후 어떤 작업도 커밋될 수 없다. 예외를 삼켜도 커밋 시점에 터진다.
 *
 * <p>그래서 행 생성을 {@code REQUIRES_NEW} 로 완전히 분리한다. 여기서 실패해도 롤백되는 것은 이 내부 트랜잭션뿐이고, 호출자의 예약 트랜잭션은 멀쩡하게
 * 이어진다. (자기 호출로는 프록시가 적용되지 않으므로 반드시 별도 빈이어야 한다.)
 */
@Component
public class DailyTransferUsageInitializer {

  private static final Logger log = LoggerFactory.getLogger(DailyTransferUsageInitializer.class);

  private final DailyTransferUsageRepository repository;

  public DailyTransferUsageInitializer(DailyTransferUsageRepository repository) {
    this.repository = repository;
  }

  /**
   * 행이 없으면 0 으로 초기화한다. 동시 생성 경쟁은 정상 경로로 간주하고 조용히 넘어간다 (어차피 누군가는 만들었다).
   *
   * @return 행이 존재함이 보장되면 true
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean ensureExists(Long userId, LocalDate date, LocalDateTime now) {
    if (repository.findById_UserIdAndId_UsageDate(userId, date).isPresent()) {
      return true;
    }
    try {
      DailyTransferUsageEntity entity = new DailyTransferUsageEntity();
      DailyTransferUsageId id = new DailyTransferUsageId();
      id.setUserId(userId);
      id.setUsageDate(date);
      entity.setId(id);
      entity.setTotalAmount(0L);
      entity.setTransferCount(0);
      entity.setUpdatedAt(now);
      repository.saveAndFlush(entity);
      return true;
    } catch (DataIntegrityViolationException e) {
      log.debug("일일 사용량 행 동시 생성 감지 userId={} date={}", userId, date);
      return true;
    }
  }
}
