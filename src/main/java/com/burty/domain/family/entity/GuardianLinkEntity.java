/**
 *
 *
 * <pre>
 * <b>Description  : 가족보호 엔티티 (GuardianLinkEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.family.entity
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
package com.burty.domain.family.entity;

import com.burty.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_guardian_link")
@Getter
@Setter
@NoArgsConstructor
public class GuardianLinkEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "link_id")
  private Long linkId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "senior_user_id", nullable = false)
  private UserEntity seniorUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "guardian_user_id", nullable = false)
  private UserEntity guardianUser;

  @Enumerated(EnumType.STRING)
  @Column(name = "relation", nullable = false)
  private Relation relation;

  @Enumerated(EnumType.STRING)
  // VIEW_ALERT_AND_APPROVE 는 22자다. 길이를 명시하지 않으면 Hibernate 가 상수 길이에 맞춰 컬럼을
  // 잡는데, 마이그레이션과 어긋나면 validate 가 실패하거나 값이 잘린다. V9 와 같은 30 으로 고정한다.
  @Column(name = "permission", nullable = false, length = 30)
  private Permission permission = Permission.VIEW_ONLY;

  @Column(name = "senior_consent_id", nullable = false)
  private Long seniorConsentId;

  @Column(name = "guardian_consent_id", nullable = false)
  private Long guardianConsentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private LinkStatus status = LinkStatus.PENDING;

  @Column(name = "linked_at")
  private LocalDateTime linkedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "revoked_by")
  private RevokedBy revokedBy;

  public enum Relation {
    CHILD,
    SPOUSE,
    PARENT,
    SIBLING,
    CAREGIVER
  }

  public enum Permission {
    VIEW_ONLY,
    VIEW_AND_ALERT,
    /**
     * 조회 + 알림 + <b>고액 이체 사전 승인</b>.
     *
     * <p>기존 두 단계는 모두 사후 통지였다. 알림이 갔을 때는 이미 돈이 나간 뒤라 보이스피싱 피해를 막지 못한다. 이 권한은 설정 금액 이상의 이체를 보류시키고 보호자
     * 승인을 받게 한다.
     */
    VIEW_ALERT_AND_APPROVE
  }

  /** 이 연결이 이체 사전 승인 권한을 갖는가. */
  public boolean canApproveTransfers() {
    return permission == Permission.VIEW_ALERT_AND_APPROVE && status == LinkStatus.ACTIVE;
  }

  public enum LinkStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    REVOKED
  }

  public enum RevokedBy {
    SENIOR,
    GUARDIAN,
    SYSTEM
  }
}
