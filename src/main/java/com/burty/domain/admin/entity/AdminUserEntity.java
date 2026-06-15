/**
 *
 *
 * <pre>
 * <b>Description  : 관리 엔티티 (AdminUserEntity)</b>
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
@Table(
    name = "tbl_admin_user",
    indexes = {@Index(name = "idx_admin_username", columnList = "username", unique = true)})
@Getter
@Setter
@NoArgsConstructor
public class AdminUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "admin_id")
  private Long adminId;

  @Column(name = "username", nullable = false, length = 50, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 20)
  private AdminRole role = AdminRole.ADMIN;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AdminStatus status = AdminStatus.ACTIVE;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum AdminRole {
    ADMIN,
    SUPER_ADMIN
  }

  public enum AdminStatus {
    ACTIVE,
    SUSPENDED
  }
}
