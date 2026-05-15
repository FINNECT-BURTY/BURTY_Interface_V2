package com.burty.core.jenkins.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.burty.core.jenkins.dto.*;
import com.burty.core.jenkins.exception.JenkinsBuildException;
import com.burty.core.jenkins.exception.JenkinsConnectionException;
import com.burty.core.jenkins.exception.JenkinsJobNotFoundException;
import com.burty.core.jenkins.util.JenkinsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Jenkins REST API와 직접 통신하는 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsClient {

    private final WebClient jenkinsWebClient;

    // 모든 Job 목록 조회
    public List<JobResponse> getAllJobs() {
        JsonNode response = jenkinsWebClient.get()
            .uri("/api/json?tree=jobs[name,url,description,buildable,color,inQueue,lastBuild[number],lastSuccessfulBuild[number],lastFailedBuild[number]]")
            .retrieve()
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to fetch jobs")))
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null || !response.has("jobs")) {
            return new ArrayList<>();
        }

        JsonNode jobs = response.get("jobs");
        return StreamSupport.stream(jobs.spliterator(), false)
            .map(JenkinsUtils::convertJsonToJobResponse)
            .collect(Collectors.toList());
    }

    // 특정 Job 정보 조회
    public JobResponse getJob(String jobName) {
        JsonNode response = jenkinsWebClient.get()
            .uri("/job/{jobName}/api/json?tree=name,url,description,buildable,color,inQueue,lastBuild[number],lastSuccessfulBuild[number],lastFailedBuild[number],builds[number,url,result,building,duration,timestamp]", jobName)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsJobNotFoundException(jobName)))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to fetch job: " + jobName)))
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null) {
            throw new JenkinsJobNotFoundException(jobName);
        }

        return JenkinsUtils.convertJsonToJobResponse(response);
    }

    // Job 생성
    public void createJob(JobCreateRequest request) {
        String jobXml = JenkinsUtils.generateJobXml(request);

        jenkinsWebClient.post()
            .uri("/createItem?name={name}", request.getJobName())
            .contentType(MediaType.APPLICATION_XML)
            .bodyValue(jobXml)
            .retrieve()
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to create job: " + request.getJobName())))
            .bodyToMono(String.class)
            .block();
    }

    // Job 삭제
    public void deleteJob(String jobName) {
        jenkinsWebClient.post()
            .uri("/job/{jobName}/doDelete", jobName)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsJobNotFoundException(jobName)))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to delete job: " + jobName)))
            .bodyToMono(String.class)
            .block();
    }

    // Job 빌드 실행
    public QueueItemResponse triggerBuild(BuildRequest request) {
        String uri;
        Long queueId = null;

        if (request.getParameters() != null && !request.getParameters().isEmpty()) {
            StringBuilder paramString = new StringBuilder("?");
            request.getParameters().forEach((key, value) ->
                paramString.append(key).append("=").append(value).append("&"));
            uri = "/job/" + request.getJobName() + "/buildWithParameters" + paramString.toString();
        } else {
            uri = "/job/" + request.getJobName() + "/build";
        }

        String location = jenkinsWebClient.post()
            .uri(uri)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsJobNotFoundException(request.getJobName())))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsBuildException("Failed to trigger build for job: " + request.getJobName())))
            .toBodilessEntity()
            .map(response -> response.getHeaders().getLocation())
            .map(Objects::toString)
            .block();

        // Queue Item ID 추출 (location 헤더에서)
        if (location != null && location.contains("/queue/item/")) {
            String[] parts = location.split("/queue/item/");
            if (parts.length > 1) {
                queueId = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
            }
        }

        return QueueItemResponse.builder()
            .id(queueId)
            .jobName(request.getJobName())
            .inQueueSince(System.currentTimeMillis())
            .why("Triggered via API")
            .blocked(false)
            .buildable(true)
            .build();
    }

    // 특정 빌드 정보 조회
    public BuildResponse getBuildInfo(String jobName, int buildNumber) {
        JsonNode response = jenkinsWebClient.get()
            .uri("/job/{jobName}/{buildNumber}/api/json?tree=number,url,result,building,duration,estimatedDuration,timestamp,actions[parameters[name,value]]",
                jobName, buildNumber)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsBuildException("Build not found: " + buildNumber)))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to get build info")))
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null) {
            throw new JenkinsBuildException("Build not found: " + buildNumber);
        }

        return JenkinsUtils.convertJsonToBuildResponse(response);
    }

    // 빌드 로그 조회
    public BuildLogResponse getBuildLog(String jobName, int buildNumber) {
        String consoleOutput = jenkinsWebClient.get()
            .uri("/job/{jobName}/{buildNumber}/consoleText", jobName, buildNumber)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsBuildException("Build not found: " + buildNumber)))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to get build log")))
            .bodyToMono(String.class)
            .block();

        if (consoleOutput == null) {
            consoleOutput = "";
        }

        // 빌드 상태 확인
        BuildResponse buildInfo = getBuildInfo(jobName, buildNumber);

        return BuildLogResponse.builder()
            .jobName(jobName)
            .buildNumber(buildNumber)
            .consoleOutput(consoleOutput)
            .size((long) consoleOutput.length())
            .hasMoreData(buildInfo.getBuilding())
            .build();
    }

    // 마지막 빌드 정보 조회
    public BuildResponse getLastBuild(String jobName) {
        JsonNode response = jenkinsWebClient.get()
            .uri("/job/{jobName}/lastBuild/api/json?tree=number,url,result,building,duration,estimatedDuration,timestamp,actions[parameters[name,value]]",
                jobName)
            .retrieve()
            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse ->
                Mono.error(new JenkinsBuildException("No builds found for job: " + jobName)))
            .onStatus(status -> status.isError(), clientResponse ->
                Mono.error(new JenkinsConnectionException("Failed to get last build")))
            .bodyToMono(JsonNode.class)
            .block();

        if (response == null) {
            throw new JenkinsBuildException("No builds found for job: " + jobName);
        }

        return JenkinsUtils.convertJsonToBuildResponse(response);
    }

    // 모든 노드 정보 조회
    public List<NodeResponse> getAllNodes() {
        try {
            JsonNode response = jenkinsWebClient.get()
                .uri("/computer/api/json?tree=computer[displayName,description,offline,temporarilyOffline,numExecutors,idle,offlineCauseReason]")
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                    Mono.error(new JenkinsConnectionException("Failed to get all nodes")))
                .bodyToMono(JsonNode.class)
                .block();

            if (response == null || !response.has("computer")) {
                return new ArrayList<>();
            }

            JsonNode computers = response.get("computer");
            return StreamSupport.stream(computers.spliterator(), false)
                .map(JenkinsUtils::convertJsonToNodeResponse)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get all nodes", e);
            throw new JenkinsConnectionException("Failed to get all nodes", e);
        }
    }

    // Jenkins 서버 연결 상태 확인
    public boolean isRunning() {
        JsonNode response = jenkinsWebClient.get()
            .uri("/api/json")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        return response != null;
    }
}
