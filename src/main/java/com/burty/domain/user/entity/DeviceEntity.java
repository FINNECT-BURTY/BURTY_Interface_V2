/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (DeviceEntity)</b>
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
@Table(
    name = "tbl_device",
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_device_token_hash", columnNames = "device_token_hash")
    },
    indexes = {
      @Index(name = "idx_device_user_fingerprint", columnList = "user_id, device_fingerprint")
    })
@Getter
@Setter
@NoArgsConstructor
public class DeviceEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "device_id")
  private Long deviceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "device_fingerprint", nullable = false, length = 64)
  private String deviceFingerprint;

  @Column(name = "device_name", length = 100)
  private String deviceName;

  /**
   * 기기 신뢰 토큰의 해시. 기기는 이 값으로만 찾는다.
   *
   * <p>평문은 두지 않는다. 예전에는 같은 행에 평문({@code device_token})도 저장해 해시로 저장한 의미가 없었다 — DB 가 읽히면 모든 기기 토큰이
   * 그대로 드러났다. 컬럼은 V11 에서 지웠다.
   */
  @Column(name = "device_token_hash", length = 64, nullable = false)
  private String deviceTokenHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "platform", nullable = false)
  private Platform platform;

  @Column(name = "os_version")
  private String osVersion;

  @Column(name = "app_version")
  private String appVersion;

  @Column(name = "fcm_token")
  private String fcmToken;

  @Column(name = "is_trusted", nullable = false)
  private Boolean isTrusted = false;

  @Column(name = "last_seen_at")
  private LocalDateTime lastSeenAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  public enum Platform {
    IOS,
    ANDROID,
    WEB
  }
}
