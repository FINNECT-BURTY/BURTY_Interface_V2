package com.berty.core.jenkins.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Jenkins Node 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResponse {

    /**
     * 노드 이름
     */
    private String name;

    /**
     * 노드 설명
     */
    private String description;

    /**
     * 온라인 여부
     */
    private Boolean online;

    /**
     * 임시 오프라인 여부
     */
    private Boolean temporarilyOffline;

    /**
     * 실행 가능 수
     */
    private Integer numExecutors;

    /**
     * 유휴 상태 여부
     */
    private Boolean idle;

    /**
     * 오프라인 원인
     */
    private String offlineCauseReason;

    /**
     * 노드 레이블
     */
    private String labelString;
}
