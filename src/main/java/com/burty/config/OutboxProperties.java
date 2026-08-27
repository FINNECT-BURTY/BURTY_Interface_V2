package com.burty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "burty.outbox")
@Getter
@Setter
public class OutboxProperties {

  /** 릴레이 활성화 여부. */
  private boolean enabled = true;

  /** 한 번에 처리할 이벤트 수. */
  private int batchSize = 50;

  /** 이 횟수만큼 실패하면 DEAD 로 격리한다 (사람이 확인해야 함). */
  private int maxAttempts = 8;

  /** 재시도 백오프 기준 초. 실제 지연 = base * 2^(attempts-1), 상한 maxBackoffSeconds. */
  private long backoffBaseSeconds = 5;

  private long maxBackoffSeconds = 900;

  /** DEAD 이벤트가 이 수를 넘으면 WARN 을 남긴다 (Grafana 알람 훅). */
  private long deadLetterWarnThreshold = 1;
}
