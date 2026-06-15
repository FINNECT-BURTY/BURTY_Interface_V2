/**
 *
 *
 * <pre>
 * <b>Description  : 상담 엔티티 (ReportSectionEntity)</b>
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

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_report_section")
public class ReportSectionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "section_id")
  private Long sectionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "report_id", nullable = false)
  private MonthlyReportEntity report;

  @Enumerated(EnumType.STRING)
  @Column(name = "section_type", nullable = false)
  private SectionType sectionType;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "easy_read_text", nullable = false, columnDefinition = "TEXT")
  private String easyReadText;

  @Column(name = "raw_data", nullable = false, columnDefinition = "JSON")
  private String rawData;

  @Enumerated(EnumType.STRING)
  @Column(name = "traffic_light", nullable = false)
  private TrafficLight trafficLight;

  @Column(name = "action_label")
  private String actionLabel;

  @Column(name = "action_payload", columnDefinition = "JSON")
  private String actionPayload;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public enum SectionType {
    SUMMARY,
    SPENDING,
    ASSETS,
    WARNING,
    ACTION,
    ACHIEVEMENT
  }

  public enum TrafficLight {
    GREEN,
    YELLOW,
    RED
  }
}
