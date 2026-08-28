package com.burty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 필드 암호화 키 로테이션 설정. */
@Component
@ConfigurationProperties(prefix = "burty.security.field-encryption-rotation")
@Getter
@Setter
public class FieldEncryptionProperties {

  /**
   * 로테이션 배치 활성화 여부.
   *
   * <p>기본값은 false 다. 키를 바꾸지 않았는데 배치가 돌면 아무 일도 하지 않지만, 매번 전체 테이블을 훑는 비용은 든다. 로테이션을 시작할 때만 켠다.
   */
  private boolean enabled = false;

  /** 한 번에 처리할 행 수. */
  private int batchSize = 200;

  /** 한 주기에 처리할 최대 행 수. 운영 중 부하를 제한한다. */
  private int maxRowsPerRun = 2000;
}
