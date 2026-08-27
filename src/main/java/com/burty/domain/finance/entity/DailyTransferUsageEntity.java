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

  /**
   * 낙관적 잠금 버전.
   *
   * <p>일일 한도 검사는 "읽고 → 검사하고 → 더한다" 이므로 동시 요청에 취약하다. 조건부 UPDATE 나 SELECT FOR UPDATE 는 DB 엔진의 MVCC/격리
   * 수준 구현에 따라 결과가 달라진다 (실제로 동시성 테스트에서 한도 10건에 12~14건이 통과했다).
   *
   * <p>{@code @Version} 은 JPA 가 UPDATE 의 WHERE 절에 버전을 넣고 영향 행 수를 확인하는 방식이라, 엔진 구현과 무관하게 잃어버린
   * 갱신(lost update)을 반드시 잡아낸다. 충돌 시 호출자가 재시도한다.
   */
  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
