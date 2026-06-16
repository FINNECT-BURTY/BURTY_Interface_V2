/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (UserProfileEntity)</b>
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tbl_user_profile")
@Getter
@Setter
public class UserProfileEntity {
  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private UserEntity user;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "birthdate", nullable = false)
  private LocalDate birthdate;

  @Column(name = "age_range")
  private Integer ageRange;

  @Enumerated(EnumType.STRING)
  @Column(name = "ux_mode", nullable = false)
  private UxMode uxMode = UxMode.STANDARD;

  @Column(name = "font_scale", nullable = false, precision = 3, scale = 2)
  private BigDecimal fontScale = BigDecimal.valueOf(1.0);

  @Column(name = "voice_enabled", nullable = false)
  private Boolean voiceEnabled = false;

  @Column(name = "preferences", columnDefinition = "JSON")
  private String preferences;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum UxMode {
    SENIOR,
    STANDARD
  }
}
