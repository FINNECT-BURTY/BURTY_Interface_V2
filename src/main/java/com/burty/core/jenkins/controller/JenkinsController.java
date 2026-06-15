/**
 *
 *
 * <pre>
 * <b>Description  : 코어 API 컨트롤러 (JenkinsController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.controller
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
package com.burty.core.jenkins.controller;

import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.core.jenkins.dto.*;
import com.burty.core.jenkins.service.JenkinsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Jenkins 관리 REST API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/jenkins")
@RequiredArgsConstructor
@Tag(name = "Jenkins Management", description = "Jenkins 관리 API")
public class JenkinsController extends BaseController {

  private final JenkinsService jenkinsService;

  // Jenkins 서버 연결 상태 확인
  @GetMapping("/health")
  @Operation(summary = "Jenkins 서버 연결 상태 확인", description = "Jenkins 서버가 정상적으로 연결되어 있는지 확인합니다.")
  public ResponseEntity<ApiResponse<Boolean>> checkHealth() {

    boolean isRunning = jenkinsService.checkConnection();

    return ResponseEntity.ok(
        ApiResponse.success(isRunning, "Jenkins server health check completed"));
  }

  // 모든 Job 목록 조회
  @GetMapping("/jobs")
  @Operation(summary = "모든 Job 목록 조회", description = "Jenkins에 등록된 모든 Job 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobs() {

    List<JobResponse> jobs = jenkinsService.getAllJobs();

    return ResponseEntity.ok(ApiResponse.success(jobs, "Successfully fetched all jobs"));
  }

  // 특정 Job 정보 조회
  @GetMapping("/jobs/{jobName}")
  @Operation(summary = "특정 Job 정보 조회", description = "지정한 이름의 Job 정보를 상세 조회합니다.")
  public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable String jobName) {

    JobResponse job = jenkinsService.getJob(jobName);

    return ResponseEntity.ok(ApiResponse.success(job, "Successfully fetched job"));
  }

  // Job 생성
  @PostMapping("/jobs")
  @Operation(summary = "Job 생성", description = "새로운 Jenkins Job을 생성합니다.")
  public ResponseEntity<ApiResponse<Void>> createJob(@Valid @RequestBody JobCreateRequest request) {

    jenkinsService.createJob(request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(null, "Successfully created job"));
  }

  // Job 업데이트
  @PutMapping("/jobs/{jobName}")
  @Operation(summary = "Job 업데이트", description = "기존 Jenkins Job의 설정을 업데이트합니다.")
  public ResponseEntity<ApiResponse<Void>> updateJob(
      @PathVariable String jobName, @Valid @RequestBody JobCreateRequest request) {

    jenkinsService.updateJob(jobName, request);

    return ResponseEntity.ok(ApiResponse.success(null, "Successfully updated job"));
  }

  // Job 삭제
  @DeleteMapping("/jobs/{jobName}")
  @Operation(summary = "Job 삭제", description = "지정한 Jenkins Job을 삭제합니다.")
  public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String jobName) {

    jenkinsService.deleteJob(jobName);

    return ResponseEntity.ok(ApiResponse.success(null, "Successfully deleted job"));
  }

  // Job 빌드 실행
  @PostMapping("/jobs/{jobName}/build")
  @Operation(summary = "Job 빌드 실행", description = "지정한 Job의 빌드를 실행합니다.")
  public ResponseEntity<ApiResponse<QueueItemResponse>> triggerBuild(
      @PathVariable String jobName, @RequestBody(required = false) BuildRequest request) {

    if (request == null) {
      request = BuildRequest.builder().jobName(jobName).build();
    } else {
      request =
          BuildRequest.builder()
              .jobName(jobName)
              .parameters(request.getParameters())
              .waitForCompletion(request.getWaitForCompletion())
              .build();
    }

    QueueItemResponse queueItem = jenkinsService.triggerBuild(request);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success(queueItem, "Successfully triggered build"));
  }

  // Job의 모든 빌드 목록 조회
  @GetMapping("/jobs/{jobName}/builds")
  @Operation(summary = "Job의 모든 빌드 목록 조회", description = "지정한 Job의 모든 빌드 목록을 조회합니다.")
  public ResponseEntity<ApiResponse<List<BuildResponse>>> getAllBuilds(
      @PathVariable String jobName) {

    List<BuildResponse> builds = jenkinsService.getAllBuilds(jobName);

    return ResponseEntity.ok(ApiResponse.success(builds, "Successfully fetched all builds"));
  }

  // 특정 빌드 정보 조회
  @GetMapping("/jobs/{jobName}/builds/{buildNumber}")
  @Operation(summary = "특정 빌드 정보 조회", description = "지정한 Job의 특정 빌드 정보를 조회합니다.")
  public ResponseEntity<ApiResponse<BuildResponse>> getBuildInfo(
      @PathVariable String jobName, @PathVariable int buildNumber) {

    BuildResponse build = jenkinsService.getBuildInfo(jobName, buildNumber);

    return ResponseEntity.ok(ApiResponse.success(build, "Successfully fetched build info"));
  }

  // 마지막 빌드 정보 조회
  @GetMapping("/jobs/{jobName}/builds/last")
  @Operation(summary = "마지막 빌드 정보 조회", description = "지정한 Job의 마지막 빌드 정보를 조회합니다.")
  public ResponseEntity<ApiResponse<BuildResponse>> getLastBuild(@PathVariable String jobName) {

    BuildResponse build = jenkinsService.getLastBuild(jobName);

    return ResponseEntity.ok(ApiResponse.success(build, "Successfully fetched last build"));
  }

  // 빌드 로그 조회
  @GetMapping("/jobs/{jobName}/builds/{buildNumber}/log")
  @Operation(summary = "빌드 로그 조회", description = "지정한 빌드의 콘솔 로그를 조회합니다.")
  public ResponseEntity<ApiResponse<BuildLogResponse>> getBuildLog(
      @PathVariable String jobName, @PathVariable int buildNumber) {

    BuildLogResponse log = jenkinsService.getBuildLog(jobName, buildNumber);

    return ResponseEntity.ok(ApiResponse.success(log, "Successfully fetched build log"));
  }

  // 모든 노드 정보 조회
  @GetMapping("/nodes")
  @Operation(summary = "모든 노드 정보 조회", description = "Jenkins에 등록된 모든 노드(에이전트) 정보를 조회합니다.")
  public ResponseEntity<ApiResponse<List<NodeResponse>>> getAllNodes() {

    List<NodeResponse> nodes = jenkinsService.getAllNodes();

    return ResponseEntity.ok(ApiResponse.success(nodes, "Successfully fetched all nodes"));
  }
}
