package com.burty.adapter.out.queue;

import com.burty.application.port.out.queue.AsyncJobPort;
import com.burty.application.port.out.queue.AsyncJobType;
import com.burty.config.QueueProperties;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "burty.redis", name = "enabled", havingValue = "true")
public class RedisStreamAsyncJobAdapter implements AsyncJobPort {

  private final StringRedisTemplate redisTemplate;
  private final QueueProperties queueProperties;

  public RedisStreamAsyncJobAdapter(
      StringRedisTemplate redisTemplate, QueueProperties queueProperties) {
    this.redisTemplate = redisTemplate;
    this.queueProperties = queueProperties;
  }

  @Override
  public void publish(AsyncJobType type, Map<String, String> payload) {
    Map<String, String> record = new HashMap<>(payload);
    record.put("type", type.name());
    record.put("createdAt", Instant.now().toString());
    String streamKey = queueProperties.streamKey(type.name());
    redisTemplate.opsForStream().add(StreamRecords.mapBacked(record).withStreamKey(streamKey));
  }

  @Override
  public boolean isEnabled() {
    return queueProperties.isEnabled();
  }
}
