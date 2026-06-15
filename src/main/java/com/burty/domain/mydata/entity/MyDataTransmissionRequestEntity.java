package com.burty.domain.mydata.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 마이데이터 정보전송요구. */
@Entity
@Table(
    name = "tbl_mydata_transmission_request",
    indexes = {
      @Index(name = "idx_md_tx_req_user", columnList = "user_id"),
      @Index(name = "idx_md_tx_req_inst", columnList = "user_id,institution_code")
    })
@Getter
@Setter
@NoArgsConstructor
public class MyDataTransmissionRequestEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "request_id")
  private Long requestId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "institution_code", nullable = false, length = 64)
  private String institutionCode;

  @Column(name = "scope", nullable = false, length = 500)
  private String scope;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private Status status = Status.REQUESTED;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  @Column(name = "authorized_at")
  private LocalDateTime authorizedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "consent_expires_at")
  private LocalDateTime consentExpiresAt;

  public enum Status {
    REQUESTED,
    AUTHORIZED,
    ACTIVE,
    REVOKED,
    EXPIRED,
    FAILED
  }
}
