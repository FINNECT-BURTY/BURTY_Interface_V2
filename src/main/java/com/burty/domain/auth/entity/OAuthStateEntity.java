/**
 *
 *
 * <pre>
 * <b>Description  : 인증 엔티티 (OAuthStateEntity)</b>
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
    name = "tbl_oauth_state",
    indexes = {@Index(name = "idx_oauth_state_expires", columnList = "expires_at")})
@Getter
@Setter
@NoArgsConstructor
public class OAuthStateEntity {

  @Id
  @Column(name = "state_key", length = 128)
  private String stateKey;

  @Column(name = "provider", length = 20, nullable = false)
  private String provider;

  /** 로그인 완료 후 redirect 할 프론트 origin. */
  @Column(name = "frontend_origin", length = 255)
  private String frontendOrigin;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
