package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.queue")
public class QueueProperties {

  private boolean enabled = true;
  private String streamPrefix = "burty:stream:";
  private String consumerGroup = "burty-workers";
  private String consumerName = "burty-worker-1";
  private long pollIntervalMs = 1000;

  /** 이 횟수만큼 전달됐는데도 처리에 실패하면 DLQ 로 보낸다. */
  private int maxDeliveries = 5;

  /** 컨슈머가 죽어 pending 으로 남은 메시지를 회수하기까지의 유휴 시간. */
  private long claimMinIdleMs = 60_000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getStreamPrefix() {
    return streamPrefix;
  }

  public void setStreamPrefix(String streamPrefix) {
    this.streamPrefix = streamPrefix;
  }

  public String getConsumerGroup() {
    return consumerGroup;
  }

  public void setConsumerGroup(String consumerGroup) {
    this.consumerGroup = consumerGroup;
  }

  public String getConsumerName() {
    return consumerName;
  }

  public void setConsumerName(String consumerName) {
    this.consumerName = consumerName;
  }

  public long getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(long pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public int getMaxDeliveries() {
    return maxDeliveries;
  }

  public void setMaxDeliveries(int maxDeliveries) {
    this.maxDeliveries = maxDeliveries;
  }

  public long getClaimMinIdleMs() {
    return claimMinIdleMs;
  }

  public void setClaimMinIdleMs(long claimMinIdleMs) {
    this.claimMinIdleMs = claimMinIdleMs;
  }

  public String streamKey(String jobType) {
    return streamPrefix + jobType.toLowerCase();
  }

  /** 처리에 반복 실패한 메시지를 격리하는 DLQ 스트림 키. */
  public String deadLetterKey(String jobType) {
    return streamPrefix + "dlq:" + jobType.toLowerCase();
  }
}
