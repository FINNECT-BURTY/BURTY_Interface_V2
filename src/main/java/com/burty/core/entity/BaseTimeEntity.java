/**
 *
 *
 * <pre>
 * <b>Description  : 코어 엔티티 (BaseTimeEntity)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.entity
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
package com.burty.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

  @CreatedDate
  @Column(name = "CREATE_DT", updatable = false, nullable = false)
  private LocalDateTime createDt;

  @LastModifiedDate
  @Column(name = "UPDATE_DT", nullable = false)
  private LocalDateTime updateDt;

  public LocalDateTime getCreateDt() {
    return createDt;
  }

  public void setCreateDt(LocalDateTime createDt) {
    this.createDt = createDt;
  }

  public LocalDateTime getUpdateDt() {
    return updateDt;
  }

  public void setUpdateDt(LocalDateTime updateDt) {
    this.updateDt = updateDt;
  }
}
