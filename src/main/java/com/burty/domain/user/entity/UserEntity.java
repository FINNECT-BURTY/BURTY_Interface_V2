/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (UserEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.user.entity
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
package com.burty.domain.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_user")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "ci_hash", nullable = false, length = 64, unique = true)
  private String ciHash;

  @Column(name = "ci", nullable = false, length = 255)
  private String ci;

  @Column(name = "phone_hash", nullable = false, length = 64, unique = true)
  private String phoneHash;

  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "last_login_ip")
  private byte[] lastLoginIp;

  @Column(name = "failed_login_count", nullable = false)
  private Integer failedLoginCount = 0;

  @Column(name = "withdrawn_at")
  private LocalDateTime withdrawnAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN
  }
}
