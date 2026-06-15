/**
 *
 *
 * <pre>
 * <b>Description  : 금융 엔티티 (DailyTransferUsageEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.finance.entity
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
package com.burty.domain.finance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_daily_transfer_usage")
@Getter
@Setter
@NoArgsConstructor
public class DailyTransferUsageEntity {
  @EmbeddedId private DailyTransferUsageId id;

  @Column(name = "total_amount", nullable = false)
  private Long totalAmount;

  @Column(name = "transfer_count", nullable = false)
  private Integer transferCount;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
