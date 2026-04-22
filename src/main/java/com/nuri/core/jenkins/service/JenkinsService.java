package com.nuri.core.jenkins.service;

import com.nuri.core.jenkins.dto.*;

import java.util.List;

/**
 * Jenkins 관리 서비스 인터페이스
 */
public interface JenkinsService {

    // 모든 Job 목록 조회
    List<JobResponse> getAllJobs();

    // 특정 Job 정보 조회
    JobResponse getJob(String jobName);

    // Job 생성
    void createJob(JobCreateRequest request);

    // Job 업데이트
    void updateJob(String jobName, JobCreateRequest request);

    // Job 삭제
    void deleteJob(String jobName);

    // Job 빌드 실행
    QueueItemResponse triggerBuild(BuildRequest request);


    // 특정 빌드 정보 조회
    BuildResponse getBuildInfo(String jobName, int buildNumber);


    // 마지막 빌드 정보 조회
    BuildResponse getLastBuild(String jobName);


    // 빌드 로그 조회
    BuildLogResponse getBuildLog(String jobName, int buildNumber);

    //Job의 모든 빌드 목록 조회
    List<BuildResponse> getAllBuilds(String jobName);

    //모든 노드 정보 조회
    List<NodeResponse> getAllNodes();

    // Jenkins 서버 연결 상태 확인
    boolean checkConnection();
}
