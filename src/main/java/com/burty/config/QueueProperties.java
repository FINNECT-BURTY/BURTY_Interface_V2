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

  public String streamKey(String jobType) {
    return streamPrefix + jobType.toLowerCase();
  }
}
