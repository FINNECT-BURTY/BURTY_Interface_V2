package com.burty.domain.mydata.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 마이데이터 기관별 동의 이력. */
@Entity
@Table(
    name = "tbl_mydata_consent_history",
    indexes = {@Index(name = "idx_md_consent_user", columnList = "user_id")})
@Getter
@Setter
@NoArgsConstructor
public class MyDataConsentHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "consent_history_id")
  private Long consentHistoryId;

  @Column(name = "transmission_request_id")
  private Long transmissionRequestId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "institution_code", nullable = false, length = 64)
  private String institutionCode;

  @Column(name = "scope", nullable = false, length = 500)
  private String scope;

  @Column(name = "consent_version", nullable = false, length = 20)
  private String consentVersion;

  @Column(name = "agreed_at", nullable = false)
  private LocalDateTime agreedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "revoke_reason", length = 200)
  private String revokeReason;
}
