package com.burty.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개인정보 파기 요청 추적.
 *
 * <p>탈퇴 처리는 한 번에 끝나지 않는다. 직접 식별정보(CI, 전화번호, 이름, 생년월일)는 즉시 익명화하지만, 전자금융거래 기록과 감사 로그는 <b>법정 보존의무</b>가
 * 있어 바로 지울 수 없다. 그래서 "언제 무엇을 지웠고, 무엇이 언제까지 남는지" 를 이 테이블이 기록한다.
 *
 * <p>이 기록 자체가 정보주체의 파기 요청에 대한 처리 증빙이 된다.
 */
@Entity
@Table(
    name = "tbl_data_erasure_request",
    indexes = {
      @Index(name = "idx_erasure_user", columnList = "user_id"),
      @Index(name = "idx_erasure_due", columnList = "status, retention_until")
    })
@Getter
@Setter
@NoArgsConstructor
public class DataErasureRequestEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "erasure_id")
  private Long erasureId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 30)
  private Reason reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private Status status = Status.IMMEDIATE_DONE;

  @Column(name = "requested_at", nullable = false)
  private LocalDateTime requestedAt;

  /** 직접 식별정보 익명화 완료 시각. */
  @Column(name = "anonymized_at")
  private LocalDateTime anonymizedAt;

  /** 법정 보존기간 종료 시각. 이후 잔여 거래·감사 기록을 파기한다. */
  @Column(name = "retention_until", nullable = false)
  private LocalDateTime retentionUntil;

  @Column(name = "purged_at")
  private LocalDateTime purgedAt;

  /** 무엇을 지웠는지 요약 (건수). 증빙용. */
  @Column(name = "summary", length = 1000)
  private String summary;

  public enum Reason {
    /** 사용자 탈퇴. */
    WITHDRAWAL,
    /** 정보주체의 파기 요청 (탈퇴 없이). */
    SUBJECT_REQUEST,
    /** 보관기간 경과에 따른 자동 파기. */
    RETENTION_EXPIRED
  }

  public enum Status {
    /** 직접 식별정보 익명화 완료. 법정 보존 대상만 남아 있음. */
    IMMEDIATE_DONE,
    /** 보존기간까지 만료되어 잔여 데이터도 파기 완료. */
    FULLY_PURGED
  }
}
