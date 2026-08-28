package com.burty.config;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * JVM 기본 시간대가 애플리케이션 {@link Clock} 과 같은지 확인한다.
 *
 * <p>도메인 로직은 주입된 {@code Clock}(Asia/Seoul)을 쓰도록 되어 있지만, 실제로는 {@code LocalDateTime.now()} 를 직접 부르는
 * 자리가 많이 남아 있다. 엔티티의 {@code @PrePersist} 처럼 빈을 받을 수 없는 곳도 있다.
 *
 * <p>지금 그 코드가 올바르게 도는 이유는 컨테이너의 시간대가 Asia/Seoul 로 맞춰져 있기 때문이다. <b>코드가 옳아서가 아니라 환경이 맞아서</b> 도는 상태다.
 * 시간대가 다른 환경에 올리면 같은 시각이 9시간 어긋나 기록되고, 한도 집계·만료 판정처럼 날짜 경계가 의미를 갖는 곳이 조용히 틀어진다.
 *
 * <p>실제로 이 프로젝트는 UTC 러너에서 한도 해제 테스트가 깨지는 형태로 한 번 겪었다.
 *
 * <p>그래서 전제를 기동 시점에 확인한다. 143곳을 전부 고치는 것보다, 전제가 깨지면 즉시 드러나게 하는 편이 확실하다.
 */
@Configuration
public class TimeZoneConsistencyValidator {

  private static final Logger log = LoggerFactory.getLogger(TimeZoneConsistencyValidator.class);

  private final Clock clock;

  public TimeZoneConsistencyValidator(Clock clock) {
    this.clock = clock;
  }

  @PostConstruct
  void verify() {
    ZoneId application = clock.getZone();
    ZoneId jvm = TimeZone.getDefault().toZoneId();

    if (application.getRules().equals(jvm.getRules())) {
      log.info("시간대 확인 — 애플리케이션 {} / JVM {}", application, jvm);
      return;
    }

    // 죽이지는 않는다. 로컬 개발까지 막으면 실효보다 불편이 크고, 이 불일치는 즉시 장애로
    // 이어지기보다 기록이 서서히 어긋나는 형태로 나타난다. 대신 놓칠 수 없게 남긴다.
    log.error(
        "시간대 불일치 — 애플리케이션 Clock 은 {} 인데 JVM 기본값은 {} 다. "
            + "LocalDateTime.now() 를 직접 쓰는 코드가 {} 만큼 어긋난 시각을 기록한다. "
            + "컨테이너에 TZ 와 -Duser.timezone 을 맞출 것.",
        application,
        jvm,
        java.time.Duration.ofSeconds(
            application.getRules().getOffset(clock.instant()).getTotalSeconds()
                - jvm.getRules().getOffset(clock.instant()).getTotalSeconds()));
  }
}
