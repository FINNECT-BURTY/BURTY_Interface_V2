package com.burty.domain.mydata.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 마이데이터 전송·수신 감사 로그. */
@Entity
@Table(
    name = "tbl_mydata_transmission_log",
    indexes = {@Index(name = "idx_md_tx_log_user", columnList = "user_id,created_at")})
@Getter
@Setter
@NoArgsConstructor
public class MyDataTransmissionLogEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "log_id")
  private Long logId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "institution_code", length = 64)
  private String institutionCode;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false, length = 16)
  private Direction direction;

  @Column(name = "summary", length = 1000)
  private String summary;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public enum Direction {
    OUTBOUND,
    INBOUND
  }
}
