package com.nuri.core.jenkins.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Jenkins Job 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    // Job 이름
    private String name;

    // Job URL
    private String url;

    // Job 설명
    private String description;

    // Job 빌드 가능 여부
    private Boolean buildable;

    // 마지막 빌드 번호
    private Integer lastBuildNumber;

    // 마지막 성공한 빌드 번호
    private Integer lastSuccessfulBuildNumber;

    // 마지막 실패한 빌드 번호
    private Integer lastFailedBuildNumber;

    // Job 컬러 (빌드 상태 표시)
    private String color;

    // 큐에 있는지 여부
    private Boolean inQueue;

    // 빌드 목록
    private List<BuildResponse> builds;
}
