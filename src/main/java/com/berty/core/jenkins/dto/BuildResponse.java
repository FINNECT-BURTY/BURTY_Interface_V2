package com.berty.core.jenkins.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Jenkins Build 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildResponse {

    // 빌드 번호
    private Integer number;

    // 빌드 URL
    private String url;

    // 빌드 결과 (SUCCESS, FAILURE, UNSTABLE 등)
    private String result;

    // 빌드 진행 중 여부
    private Boolean building;

    // 빌드 소요 시간 (밀리초)
    private Long duration;

    // 예상 소요 시간 (밀리초)
    private Long estimatedDuration;

    // 빌드 시작 시간 (타임스탬프)
    private Long timestamp;

    // 빌드 파라미터
    private Map<String, String> parameters;

    // 빌드를 실행한 사용자
    private String userId;

    // 빌드 설명
    private String description;
}
