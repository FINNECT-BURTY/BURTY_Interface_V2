/**
 *
 *
 * <pre>
 * <b>Description  : 상담 엔티티 (MonthlyReportEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.consult.entity
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
package com.burty.domain.consult.entity;

import com.burty.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_monthly_report")
@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "report_id")
  private Long reportId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "period_month", nullable = false)
  private LocalDate periodMonth;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReportStatus status = ReportStatus.GENERATING;

  @Column(name = "pdf_object_key")
  private String pdfObjectKey;

  @Column(name = "pdf_size_bytes")
  private Integer pdfSizeBytes;

  @Column(name = "share_token", length = 32)
  private String shareToken;

  @Column(name = "share_expires_at")
  private LocalDateTime shareExpiresAt;

  @Column(name = "llm_invocation_id", columnDefinition = "BINARY(16)")
  private UUID llmInvocationId;

  @Column(name = "total_cost_usd", precision = 10, scale = 6)
  private BigDecimal totalCostUsd;

  @Column(name = "generated_at")
  private LocalDateTime generatedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "failed_reason")
  private String failedReason;

  public enum ReportStatus {
    GENERATING,
    READY,
    DELIVERED,
    FAILED,
    ARCHIVED
  }
}
