/**
 *
 *
 * <pre>
 * <b>Description  : 코어 (JenkinsServiceImpl)</b>
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

import com.burty.core.jenkins.client.JenkinsClient;
import com.burty.core.jenkins.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Jenkins 관리 서비스 구현체 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsServiceImpl implements JenkinsService {

  private final JenkinsClient jenkinsClient;

  @Override
  public List<JobResponse> getAllJobs() {

    return jenkinsClient.getAllJobs();
  }

  @Override
  public JobResponse getJob(String jobName) {

    return jenkinsClient.getJob(jobName);
  }

  @Override
  public void createJob(JobCreateRequest request) {

    jenkinsClient.createJob(request);
  }

  @Override
  public void updateJob(String jobName, JobCreateRequest request) {

    // 먼저 기존 Job 삭제
    jenkinsClient.deleteJob(jobName);
    // 새로운 설정으로 Job 재생성
    jenkinsClient.createJob(request);
  }

  @Override
  public void deleteJob(String jobName) {

    jenkinsClient.deleteJob(jobName);
  }

  @Override
  public QueueItemResponse triggerBuild(BuildRequest request) {

    return jenkinsClient.triggerBuild(request);
  }

  @Override
  public BuildResponse getBuildInfo(String jobName, int buildNumber) {

    return jenkinsClient.getBuildInfo(jobName, buildNumber);
  }

  @Override
  public BuildResponse getLastBuild(String jobName) {

    return jenkinsClient.getLastBuild(jobName);
  }

  @Override
  public BuildLogResponse getBuildLog(String jobName, int buildNumber) {

    return jenkinsClient.getBuildLog(jobName, buildNumber);
  }

  @Override
  public List<BuildResponse> getAllBuilds(String jobName) {

    JobResponse job = jenkinsClient.getJob(jobName);

    return job.getBuilds();
  }

  @Override
  public List<NodeResponse> getAllNodes() {

    return jenkinsClient.getAllNodes();
  }

  @Override
  public boolean checkConnection() {

    return jenkinsClient.isRunning();
  }
}
