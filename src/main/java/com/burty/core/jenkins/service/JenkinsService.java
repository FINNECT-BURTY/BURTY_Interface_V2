/**
 *
 *
 * <pre>
 * <b>Description  : 코어 애플리케이션 서비스 (JenkinsService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.service
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
package com.burty.core.jenkins.service;

import com.burty.core.jenkins.dto.*;
import java.util.List;

/** Jenkins 관리 서비스 인터페이스 */
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

  // Job의 모든 빌드 목록 조회
  List<BuildResponse> getAllBuilds(String jobName);

  // 모든 노드 정보 조회
  List<NodeResponse> getAllNodes();

  // Jenkins 서버 연결 상태 확인
  boolean checkConnection();
}
