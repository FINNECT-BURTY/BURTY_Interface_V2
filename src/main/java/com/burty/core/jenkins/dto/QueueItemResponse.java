/**
 *
 *
 * <pre>
 * <b>Description  : 코어 응답 DTO (QueueItemResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.dto
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
package com.burty.core.jenkins.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Jenkins Queue Item 정보 응답 DTO */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueItemResponse {

  /** Queue ID */
  private Long id;

  /** Job 이름 */
  private String jobName;

  /** Queue에 들어간 시간 (타임스탬프) */
  private Long inQueueSince;

  /** 대기 이유 */
  private String why;

  /** 차단 여부 */
  private Boolean blocked;

  /** 빌드 가능 여부 */
  private Boolean buildable;

  /** 실행 중인 빌드 번호 (실행된 경우) */
  private Integer executableNumber;
}
