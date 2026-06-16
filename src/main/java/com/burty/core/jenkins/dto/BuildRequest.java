/**
 *
 *
 * <pre>
 * <b>Description  : 코어 요청 DTO (BuildRequest)</b>
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
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Jenkins Build 실행 요청 DTO */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildRequest {

  // Job 이름
  @NotBlank(message = "Job name is required")
  private String jobName;

  // 빌드 파라미터
  private Map<String, String> parameters;

  // 빌드 대기 여부 (빌드 완료까지 대기)
  private Boolean waitForCompletion;
}
