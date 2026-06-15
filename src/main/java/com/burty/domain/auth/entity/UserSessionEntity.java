/**
 *
 *
 * <pre>
 * <b>Description  : 인증 엔티티 (UserSessionEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.entity
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
package com.burty.domain.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tbl_user_session",
    indexes = {
      @Index(name = "idx_session_user_active", columnList = "user_id, revoked_at"),
      @Index(name = "idx_session_refresh", columnList = "refresh_token_hash")
    })
@Getter
@Setter
@NoArgsConstructor
public class UserSessionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "session_id")
  private Long sessionId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "device_id", length = 64)
  private String deviceId;

  @Column(name = "refresh_token_hash", length = 64, nullable = false)
  private String refreshTokenHash;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
