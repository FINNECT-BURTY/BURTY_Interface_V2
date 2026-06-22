package com.burty.application.service.queue;

import com.burty.adapter.out.queue.InMemoryAsyncJobAdapter;
import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.port.out.queue.AsyncJobType;
import com.burty.application.service.notification.NotificationDispatcher;
import com.burty.config.QueueProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsyncJobConsumer {
  private static final Logger log = LoggerFactory.getLogger(AsyncJobConsumer.class);

  private final QueueProperties queueProperties;
  private final NotificationDispatcher notificationDispatcher;
  private final ObjectProvider<InMemoryAsyncJobAdapter> inMemoryAdapter;
  private final ObjectProvider<StringRedisTemplate> redisTemplate;

  public AsyncJobConsumer(
      QueueProperties queueProperties,
      @Lazy NotificationDispatcher notificationDispatcher,
      ObjectProvider<InMemoryAsyncJobAdapter> inMemoryAdapter,
      ObjectProvider<StringRedisTemplate> redisTemplate) {
    this.queueProperties = queueProperties;
    this.notificationDispatcher = notificationDispatcher;
    this.inMemoryAdapter = inMemoryAdapter;
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  void initRedisConsumerGroups() {
    StringRedisTemplate template = redisTemplate.getIfAvailable();
    if (template == null) {
      return;
    }
    for (AsyncJobType type : AsyncJobType.values()) {
      String streamKey = queueProperties.streamKey(type.name());
      try {
        template.opsForStream().createGroup(streamKey, queueProperties.getConsumerGroup());
      } catch (Exception ignored) {
        // group already exists
      }
    }
  }

  @Scheduled(fixedDelayString = "${burty.queue.poll-interval-ms:1000}")
  public void pollJobs() {
    if (!queueProperties.isEnabled()) {
      return;
    }
    InMemoryAsyncJobAdapter memory = inMemoryAdapter.getIfAvailable();
    if (memory != null) {
      Map<String, String> job;
      while ((job = memory.poll()) != null) {
        handleJob(job);
      }
      return;
    }
    pollRedisJobs();
  }

  private void pollRedisJobs() {
    StringRedisTemplate template = redisTemplate.getIfAvailable();
    if (template == null) {
      return;
    }
    for (AsyncJobType type : AsyncJobType.values()) {
      String streamKey = queueProperties.streamKey(type.name());
      List<MapRecord<String, Object, Object>> records =
          template
              .opsForStream()
              .read(
                  Consumer.from(
                      queueProperties.getConsumerGroup(), queueProperties.getConsumerName()),
                  StreamReadOptions.empty().count(10).block(Duration.ofMillis(200)),
                  StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
      if (records == null) {
        continue;
      }
      for (MapRecord<String, Object, Object> record : records) {
        Map<String, String> payload = toStringMap(record.getValue());
        handleJob(payload);
        template.opsForStream().acknowledge(streamKey, record);
      }
    }
  }

  private void handleJob(Map<String, String> payload) {
    String type = payload.get("type");
    if (type == null) {
      return;
    }
    try {
      switch (AsyncJobType.valueOf(type)) {
        case NOTIFICATION -> handleNotification(payload);
        case TRANSACTION_SYNC, TRANSFER ->
            log.debug("Async job queued type={} payload={}", type, payload);
        default -> log.warn("Unknown async job type={}", type);
      }
    } catch (IllegalArgumentException e) {
      log.warn("Invalid async job type={}", type);
    } catch (Exception e) {
      log.error("Async job failed type={} error={}", type, e.getMessage(), e);
    }
  }

  private void handleNotification(Map<String, String> payload) {
    String userId = payload.get("userId");
    String channelName = payload.get("channel");
    String title = payload.get("title");
    String body = payload.get("body");
    if (userId == null || channelName == null) {
      return;
    }
    NotificationChannelPort.Channel channel = NotificationChannelPort.Channel.valueOf(channelName);
    notificationDispatcher.dispatchDirect(userId, channel, title, body);
  }

  private static Map<String, String> toStringMap(Map<Object, Object> raw) {
    return raw.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toMap(
                e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
  }
}
