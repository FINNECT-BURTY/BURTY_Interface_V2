package com.berty.core.jenkins.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.berty.core.jenkins.dto.BuildResponse;
import com.berty.core.jenkins.dto.JobCreateRequest;
import com.berty.core.jenkins.dto.JobResponse;
import com.berty.core.jenkins.dto.NodeResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Jenkins 관련 유틸리티 클래스
 */
public class JenkinsUtils {

    private JenkinsUtils() {
        // 인스턴스 생성 방지
    }

    public static JobResponse convertJsonToJobResponse(JsonNode json) {
        List<BuildResponse> builds = new ArrayList<>();
        if (json.has("builds")) {
            JsonNode buildsNode = json.get("builds");
            builds = StreamSupport.stream(buildsNode.spliterator(), false)
                    .map(JenkinsUtils::convertJsonToBuildResponse)
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return JobResponse.builder()
                .name(json.has("name") ? json.get("name").asText() : null)
                .url(json.has("url") ? json.get("url").asText() : null)
                .description(json.has("description") ? json.get("description").asText() : null)
                .buildable(json.has("buildable") ? json.get("buildable").asBoolean() : null)
                .lastBuildNumber(json.has("lastBuild") && json.get("lastBuild").has("number") ?
                        json.get("lastBuild").get("number").asInt() : null)
                .lastSuccessfulBuildNumber(json.has("lastSuccessfulBuild") && json.get("lastSuccessfulBuild").has("number") ?
                        json.get("lastSuccessfulBuild").get("number").asInt() : null)
                .lastFailedBuildNumber(json.has("lastFailedBuild") && json.get("lastFailedBuild").has("number") ?
                        json.get("lastFailedBuild").get("number").asInt() : null)
                .color(json.has("color") ? json.get("color").asText() : null)
                .inQueue(json.has("inQueue") ? json.get("inQueue").asBoolean() : null)
                .builds(builds)
                .build();
    }

    public static BuildResponse convertJsonToBuildResponse(JsonNode json) {
        Map<String, String> parameters = new HashMap<>();

        if (json.has("actions")) {
            JsonNode actions = json.get("actions");
            for (JsonNode action : actions) {
                if (action.has("parameters")) {
                    JsonNode params = action.get("parameters");
                    for (JsonNode param : params) {
                        if (param.has("name") && param.has("value")) {
                            parameters.put(
                                    param.get("name").asText(),
                                    param.get("value").asText()
                            );
                        }
                    }
                }
            }
        }

        return BuildResponse.builder()
                .number(json.has("number") ? json.get("number").asInt() : null)
                .url(json.has("url") ? json.get("url").asText() : null)
                .result(json.has("result") && !json.get("result").isNull() ? json.get("result").asText() : null)
                .building(json.has("building") ? json.get("building").asBoolean() : null)
                .duration(json.has("duration") ? json.get("duration").asLong() : null)
                .estimatedDuration(json.has("estimatedDuration") ? json.get("estimatedDuration").asLong() : null)
                .timestamp(json.has("timestamp") ? json.get("timestamp").asLong() : null)
                .parameters(parameters)
                .build();
    }

    public static NodeResponse convertJsonToNodeResponse(JsonNode json) {
        return NodeResponse.builder()
                .name(json.has("displayName") ? json.get("displayName").asText() : null)
                .description(json.has("description") ? json.get("description").asText() : null)
                .online(json.has("offline") ? !json.get("offline").asBoolean() : null)
                .temporarilyOffline(json.has("temporarilyOffline") ? json.get("temporarilyOffline").asBoolean() : null)
                .numExecutors(json.has("numExecutors") ? json.get("numExecutors").asInt() : null)
                .idle(json.has("idle") ? json.get("idle").asBoolean() : null)
                .offlineCauseReason(json.has("offlineCauseReason") ? json.get("offlineCauseReason").asText() : null)
                .build();
    }

    public static String generateJobXml(JobCreateRequest request) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version='1.1' encoding='UTF-8'?>\n");
        xml.append("<project>\n");
        xml.append("  <description>").append(request.getDescription() != null ? request.getDescription() : "").append("</description>\n");
        xml.append("  <keepDependencies>false</keepDependencies>\n");
        xml.append("  <properties/>\n");

        // Git 설정
        if (request.getGitUrl() != null) {
            xml.append("  <scm class='hudson.plugins.git.GitSCM'>\n");
            xml.append("    <userRemoteConfigs>\n");
            xml.append("      <hudson.plugins.git.UserRemoteConfig>\n");
            xml.append("        <url>").append(request.getGitUrl()).append("</url>\n");
            xml.append("      </hudson.plugins.git.UserRemoteConfig>\n");
            xml.append("    </userRemoteConfigs>\n");
            xml.append("    <branches>\n");
            xml.append("      <hudson.plugins.git.BranchSpec>\n");
            xml.append("        <name>").append(request.getGitBranch() != null ? request.getGitBranch() : "*/main").append("</name>\n");
            xml.append("      </hudson.plugins.git.BranchSpec>\n");
            xml.append("    </branches>\n");
            xml.append("  </scm>\n");
        } else {
            xml.append("  <scm class='hudson.scm.NullSCM'/>\n");
        }

        xml.append("  <canRoam>true</canRoam>\n");
        xml.append("  <disabled>false</disabled>\n");
        xml.append("  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>\n");
        xml.append("  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n");
        xml.append("  <triggers/>\n");
        xml.append("  <concurrentBuild>false</concurrentBuild>\n");

        // 빌드 스크립트
        if (request.getBuildScript() != null) {
            xml.append("  <builders>\n");
            xml.append("    <hudson.tasks.Shell>\n");
            xml.append("      <command>").append(escapeXml(request.getBuildScript())).append("</command>\n");
            xml.append("    </hudson.tasks.Shell>\n");
            xml.append("  </builders>\n");
        } else {
            xml.append("  <builders/>\n");
        }

        xml.append("  <publishers/>\n");
        xml.append("  <buildWrappers/>\n");
        xml.append("</project>");

        return xml.toString();
    }

    public static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
