package com.burty.config;

import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 이체 정책 값.
 *
 * <p>예전에는 이 값들이 서비스 클래스의 {@code private static final} 상수였다. 보호자 알림 기준액 같은 건 서비스 운영 중 조정이 필요한 값이고,
 * 시니어 대상 서비스라면 사용자별로도 달라야 한다. 최소한 설정으로 빼서 재배포 없이 바꿀 수 있게 한다.
 */
@Component
@ConfigurationProperties(prefix = "burty.transfer")
@Getter
@Setter
public class TransferPolicyProperties {

  /** 이 금액 이상이면 보호자에게 이체 사실을 알린다. */
  private long familyAlertThreshold = 1_000_000L;

  /** 이 금액 이상이면 고액 이체로 보고 이상거래 경고를 함께 보낸다. */
  private long largeTransferThreshold = 3_000_000L;

  /** 심야 이체로 간주하는 구간 시작 (이 시각 이후). */
  private LocalTime nightStart = LocalTime.of(23, 0);

  /** 심야 이체로 간주하는 구간 종료 (이 시각 이전). */
  private LocalTime nightEnd = LocalTime.of(6, 0);

  /** 결과 불명(UNKNOWN) 건을 처음 정산 조회하기까지의 지연. */
  private long reconcileInitialDelaySeconds = 30;

  /** 정산 재조회 간격. */
  private long reconcileRetryDelaySeconds = 300;

  /** 이 횟수만큼 조회해도 확정되지 않으면 수동 처리 대상으로 경고를 올린다. */
  private int reconcileMaxAttempts = 12;

  /** EXECUTING 상태로 이 시간 넘게 머문 건은 프로세스가 죽은 것으로 보고 정산 대상에 넣는다. */
  private long stuckExecutingSeconds = 120;

  // ── 보호자 사전 승인 ────────────────────────────────────────────────────
  // 기존 가족 보호는 사후 통지뿐이라 이미 나간 돈을 막지 못했다. 아래 설정으로 차단 플로우를 켠다.

  /** 보호자 사전 승인 기능 사용 여부. */
  private boolean approvalEnabled = true;

  /** 이 금액 이상이면 승인 권한을 가진 보호자의 사전 승인을 받는다. */
  private long approvalThreshold = 500_000L;

  /** 승인 요청 유효시간(분). 지나면 자동 만료되어 이체가 취소된다. */
  private long approvalExpiryMinutes = 60;
}
