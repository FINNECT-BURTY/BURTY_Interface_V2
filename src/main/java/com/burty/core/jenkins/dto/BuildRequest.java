package com.burty.core.jenkins.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Jenkins Build 실행 요청 DTO
 */
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
