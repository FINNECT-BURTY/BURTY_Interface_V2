/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 엔티티 (ConsentRecordEntity)</b>
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
@Table(name = "tbl_consent_record")
@Getter
@Setter
@NoArgsConstructor
public class ConsentRecordEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "consent_id")
  private Long consentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Enumerated(EnumType.STRING)
  @Column(name = "consent_type", nullable = false)
  private ConsentType consentType;

  @Column(name = "consent_version", nullable = false)
  private String consentVersion;

  @Column(name = "document_hash", nullable = false, length = 64)
  private String documentHash;

  @Column(name = "agreed_at", nullable = false)
  private LocalDateTime agreedAt;

  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  @Column(name = "revoke_reason")
  private String revokeReason;

  @Column(name = "ip_address")
  private byte[] ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  public enum ConsentType {
    TERMS,
    PRIVACY,
    MYDATA,
    FAMILY_SHARE,
    MARKETING,
    THIRD_PARTY_SHARE,
    SECURITY_LOG,
    LOCATION_POLICY_MATCH
  }
}
