/**
 *
 *
 * <pre>
 * <b>Description  : 금융 (DailyTransferUsageId)</b>
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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class DailyTransferUsageId implements Serializable {
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "usage_date")
  private LocalDate usageDate;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public LocalDate getUsageDate() {
    return usageDate;
  }

  public void setUsageDate(LocalDate usageDate) {
    this.usageDate = usageDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DailyTransferUsageId that)) return false;
    return Objects.equals(userId, that.userId) && Objects.equals(usageDate, that.usageDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, usageDate);
  }
}
