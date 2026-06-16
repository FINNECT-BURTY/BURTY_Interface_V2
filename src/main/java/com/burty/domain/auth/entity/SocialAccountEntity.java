/**
 *
 *
 * <pre>
 * <b>Description  : 인증 엔티티 (SocialAccountEntity)</b>
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
    name = "tbl_social_account",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_social_provider_subject",
          columnNames = {"provider", "provider_user_id_hash"})
    },
    indexes = {
      @Index(name = "idx_social_user", columnList = "user_id"),
      @Index(name = "idx_social_email", columnList = "email_hash")
    })
@Getter
@Setter
@NoArgsConstructor
public class SocialAccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "social_account_id")
  private Long socialAccountId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "provider", length = 20, nullable = false)
  private String provider;

  @Column(name = "provider_user_id_hash", length = 64, nullable = false)
  private String providerUserIdHash;

  @Column(name = "email_hash", length = 64)
  private String emailHash;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "linked_at", nullable = false)
  private LocalDateTime linkedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (linkedAt == null) linkedAt = now;
    if (lastLoginAt == null) lastLoginAt = now;
  }
}
