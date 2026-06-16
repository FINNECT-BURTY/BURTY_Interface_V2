/**
 *
 *
 * <pre>
 * <b>Description  : 코어 요청 DTO (JobCreateRequest)</b>
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

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Jenkins Job 생성 요청 DTO */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateRequest {

  // Job 이름
  @NotBlank(message = "Job 이름은 필수 입니다.")
  private String jobName;

  // Job 설명
  private String description;

  // Git Repository URL
  private String gitUrl;

  // Git Branch
  private String gitBranch;

  // 빌드 스크립트 (Groovy/Shell)
  private String buildScript;

  // Job 타입 (freestyle, pipeline 등)
  private String jobType;
}
