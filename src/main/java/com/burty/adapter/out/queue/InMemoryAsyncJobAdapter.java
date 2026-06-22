package com.burty.adapter.out.queue;

import com.burty.application.port.out.queue.AsyncJobPort;
import com.burty.application.port.out.queue.AsyncJobType;
import com.burty.config.QueueProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "burty.redis",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class InMemoryAsyncJobAdapter implements AsyncJobPort {

  private final QueueProperties queueProperties;
  private final ConcurrentLinkedQueue<Map<String, String>> queue = new ConcurrentLinkedQueue<>();

  public InMemoryAsyncJobAdapter(QueueProperties queueProperties) {
    this.queueProperties = queueProperties;
  }

  @Override
  public void publish(AsyncJobType type, Map<String, String> payload) {
    Map<String, String> record = new HashMap<>(payload);
    record.put("type", type.name());
    record.put("createdAt", Instant.now().toString());
    queue.offer(record);
  }

  @Override
  public boolean isEnabled() {
    return queueProperties.isEnabled();
  }

  public Map<String, String> poll() {
    return queue.poll();
  }
}
