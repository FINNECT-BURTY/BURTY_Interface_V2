package com.nuri.core.jenkins.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Jenkins Build 로그 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildLogResponse {

    // Job 이름
    private String jobName;

    // 빌드 번호
    private Integer buildNumber;

    // 빌드 로그 내용
    private String consoleOutput;

    // 로그 사이즈 (바이트)
    private Long size;

    //빌드 진행 중 여부
    private Boolean hasMoreData;
}
