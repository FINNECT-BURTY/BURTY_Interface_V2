/**
 *
 *
 * <pre>
 * <b>Description  : 관리 엔티티 (AuditLogEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.admin.entity
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.domain.admin.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_id")
  private Long auditId;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_type", nullable = false)
  private ActorType actorType;

  @Column(name = "actor_id")
  private Long actorId;

  @Column(name = "target_type", nullable = false)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "action", nullable = false)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false)
  private Result result;

  @Column(name = "ip_address")
  private byte[] ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "request_id", length = 36)
  private String requestId;

  @Column(name = "session_id", length = 36)
  private String sessionId;

  @Column(name = "before_snapshot", columnDefinition = "JSON")
  private String beforeSnapshot;

  @Column(name = "after_snapshot", columnDefinition = "JSON")
  private String afterSnapshot;

  @Column(name = "metadata", columnDefinition = "JSON")
  private String metadata;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "error_message")
  private String errorMessage;

  /**
   * 직전 감사 로그의 entryHash. 체인의 연결 고리다.
   *
   * <p>감사 로그는 "무슨 일이 있었는지" 를 사후에 증명하는 기록인데, 단순 INSERT 만으로는 DB 접근 권한이 있는 사람이 조용히 지우거나 고칠 수 있다. 각 행이
   * 직전 행의 해시를 품게 하면, 중간의 한 행만 손대도 그 이후 체인이 전부 어긋나 검증에서 드러난다.
   */
  @Column(name = "prev_hash", length = 64)
  private String prevHash;

  /** 이 행의 내용 + prevHash 로 계산한 SHA-256. */
  @Column(name = "entry_hash", length = 64)
  private String entryHash;

  /** 체인 내 순번. 행 삭제를 탐지한다 (해시만으로는 맨 끝을 통째로 잘라내는 것을 못 잡는다). */
  @Column(name = "chain_seq")
  private Long chainSeq;

  public enum ActorType {
    USER,
    GUARDIAN,
    SYSTEM,
    AI_AGENT,
    BANK,
    ADMIN,
    SCHEDULER
  }

  public enum Result {
    SUCCESS,
    FAILED,
    DENIED
  }
}
