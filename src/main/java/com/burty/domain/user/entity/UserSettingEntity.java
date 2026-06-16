/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (UserSettingEntity)</b>
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
    name = "tbl_user_setting",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_setting_key",
          columnNames = {"user_id", "setting_key"})
    })
@Getter
@Setter
@NoArgsConstructor
public class UserSettingEntity {

  @Id
  @Column(name = "setting_id", length = 200)
  private String settingId;

  @Column(name = "user_id", length = 64, nullable = false)
  private String userId;

  @Column(name = "setting_key", length = 40, nullable = false)
  private String settingKey;

  @Column(name = "setting_value_long")
  private Long settingValueLong;

  @Column(name = "setting_value_str", length = 500)
  private String settingValueStr;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  void onTouch() {
    if (settingId == null) settingId = userId + "|" + settingKey;
    updatedAt = LocalDateTime.now();
  }
}
