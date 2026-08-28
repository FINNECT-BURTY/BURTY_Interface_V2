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
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis Stream / 인메모리 큐 컨슈머.
 *
 * <p>예전 구현의 결정적 문제는 <b>처리 결과와 무관하게 항상 ACK</b> 했다는 점이다. {@code handleJob} 이 내부에서 모든 예외를 삼켰고 그 직후
 * {@code acknowledge()} 를 불렀다. 결과적으로 전송에 실패한 알림은 재시도도 DLQ 도 없이 영구 유실됐다. Redis Stream 을 쓰는 이유의 절반을
 * 버리고 있었던 셈이다.
 *
 * <p>지금은
 *
 * <ul>
 *   <li>처리에 <b>성공했을 때만</b> ACK 한다. 실패하면 pending 으로 남아 재전달된다.
 *   <li>전달 횟수가 임계치를 넘으면 DLQ 스트림으로 옮기고 ACK 한다 (무한 재시도 방지).
 *   <li>죽은 컨슈머가 물고 있던 pending 메시지를 {@code XAUTOCLAIM} 으로 회수한다.
 * </ul>
 */
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
        try {
          handleJob(job);
        } catch (RuntimeException e) {
          // 인메모리 큐는 개발 편의용이라 재시도 큐가 없다. 최소한 유실 사실은 남긴다.
          log.error(
              "인메모리 비동기 작업 처리 실패 — 유실됨 type={} reason={}", job.get("type"), e.getMessage(), e);
        }
      }
      return;
    }
    reclaimStalledMessages();
    pollRedisJobs();
  }

  /**
   * 컨슈머가 죽어 pending 으로 남은 메시지를 회수한다.
   *
   * <p>이게 없으면 배포나 크래시 때 처리 중이던 메시지가 원래 컨슈머 이름에 영원히 묶인 채 아무도 처리하지 않는다. 그룹 전체의 pending 을 보고 충분히 오래 유휴
   * 상태인 것만 이 컨슈머로 가져온다.
   */
  private void reclaimStalledMessages() {
    StringRedisTemplate template = redisTemplate.getIfAvailable();
    if (template == null) {
      return;
    }
    Duration minIdle = Duration.ofMillis(queueProperties.getClaimMinIdleMs());
    for (AsyncJobType type : AsyncJobType.values()) {
      String streamKey = queueProperties.streamKey(type.name());
      try {
        PendingMessages pending =
            template
                .opsForStream()
                .pending(streamKey, queueProperties.getConsumerGroup(), Range.unbounded(), 10L);
        if (pending == null || pending.isEmpty()) {
          continue;
        }
        RecordId[] stale =
            pending.stream()
                .filter(m -> m.getElapsedTimeSinceLastDelivery().compareTo(minIdle) >= 0)
                .map(PendingMessage::getId)
                .toArray(RecordId[]::new);
        if (stale.length == 0) {
          continue;
        }
        List<MapRecord<String, Object, Object>> claimed =
            template
                .opsForStream()
                .claim(
                    streamKey,
                    queueProperties.getConsumerGroup(),
                    queueProperties.getConsumerName(),
                    minIdle,
                    stale);
        if (claimed != null && !claimed.isEmpty()) {
          log.info("정체된 메시지 회수 stream={} count={}", streamKey, claimed.size());
          processRecords(template, streamKey, claimed);
        }
      } catch (RuntimeException e) {
        log.warn("pending 메시지 회수 실패 stream={} reason={}", streamKey, e.getMessage());
      }
    }
  }

  private void pollRedisJobs() {
    StringRedisTemplate template = redisTemplate.getIfAvailable();
    if (template == null) {
      return;
    }
    for (AsyncJobType type : AsyncJobType.values()) {
      String streamKey = queueProperties.streamKey(type.name());
      // 블로킹 읽기를 쓰지 않는다. StringRedisTemplate 은 네이티브 커넥션을 공유하므로
      // XREADGROUP BLOCK 이 그 커넥션을 붙잡고 있는 동안 다른 Redis 명령이 뒤에서 대기한다.
      // 작업 타입마다 200ms 씩 잡으면 폴링 한 번이 타입 수만큼 커넥션을 막는다.
      // 어차피 이 메서드는 1초 주기로 다시 호출되므로 기다릴 이유가 없다.
      List<MapRecord<String, Object, Object>> records =
          template
              .opsForStream()
              .read(
                  Consumer.from(
                      queueProperties.getConsumerGroup(), queueProperties.getConsumerName()),
                  StreamReadOptions.empty().count(10),
                  StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
      if (records == null) {
        continue;
      }
      processRecords(template, streamKey, records);
    }
  }

  /** 성공했을 때만 ACK 한다. 실패하면 pending 으로 남겨 재전달되게 하고, 전달 횟수가 임계치를 넘으면 DLQ 로 격리한다. */
  private void processRecords(
      StringRedisTemplate template,
      String streamKey,
      List<MapRecord<String, Object, Object>> records) {
    for (MapRecord<String, Object, Object> record : records) {
      Map<String, String> payload = toStringMap(record.getValue());
      try {
        handleJob(payload);
        ack(template, streamKey, record);
      } catch (RuntimeException e) {
        long deliveries = deliveryCount(template, streamKey, record.getId().getValue());
        if (deliveries >= queueProperties.getMaxDeliveries()) {
          moveToDeadLetter(template, streamKey, record, payload, e);
        } else {
          log.warn(
              "비동기 작업 처리 실패 — 재전달 예정 stream={} id={} deliveries={}/{} reason={}",
              streamKey,
              record.getId(),
              deliveries,
              queueProperties.getMaxDeliveries(),
              e.getMessage(),
              e);
        }
      }
    }
  }

  /**
   * 처리 완료를 확정한다.
   *
   * <p>반드시 (스트림, 그룹, 레코드ID) 를 명시한다. {@code acknowledge(String, Record)} 오버로드는 <b>첫 인자가 그룹명</b>이라,
   * 스트림 키를 넘기면 존재하지 않는 그룹에 XACK 하게 된다. Redis 는 그것을 오류로 보지 않고 0 을 돌려주므로 아무 일도 일어나지 않은 채 성공처럼 보인다.
   *
   * <p>그래서 어떤 메시지도 확정되지 않았고, pending 에 남아 회수 주기마다 다시 처리됐다 — 사용자에게 같은 알림이 반복 발송되고, 전달 횟수가 임계치를 넘으면
   * DLQ 항목까지 계속 쌓였다.
   */
  private void ack(StringRedisTemplate template, String streamKey, MapRecord<String, ?, ?> record) {
    Long acknowledged =
        template
            .opsForStream()
            .acknowledge(streamKey, queueProperties.getConsumerGroup(), record.getId());

    // XACK 은 대상이 없어도 오류가 아니라 0 을 돌려준다. 확인하지 않으면 확정되지 않은
    // 메시지를 확정된 것으로 착각한 채 넘어가고, 그 메시지는 pending 에 남아 계속 재전달된다.
    if (acknowledged == null || acknowledged == 0L) {
      log.error(
          "ACK 되지 않았다 — 메시지가 pending 에 남아 재전달된다 stream={} group={} id={}",
          streamKey,
          queueProperties.getConsumerGroup(),
          record.getId());
    }
  }

  private long deliveryCount(StringRedisTemplate template, String streamKey, String id) {
    try {
      PendingMessages pending =
          template
              .opsForStream()
              .pending(streamKey, queueProperties.getConsumerGroup(), Range.closed(id, id), 1L);
      return pending == null || pending.isEmpty() ? 1L : pending.get(0).getTotalDeliveryCount();
    } catch (RuntimeException e) {
      // 전달 횟수를 못 읽으면 보수적으로 1 로 본다 (성급한 DLQ 격리 방지).
      return 1L;
    }
  }

  private void moveToDeadLetter(
      StringRedisTemplate template,
      String streamKey,
      MapRecord<String, Object, Object> record,
      Map<String, String> payload,
      RuntimeException cause) {
    String dlq = queueProperties.deadLetterKey(typeOf(payload));
    Map<String, String> entry = new java.util.HashMap<>(payload);
    entry.put("_originalStream", streamKey);
    entry.put("_originalId", record.getId().getValue());
    entry.put("_error", cause.getClass().getSimpleName() + ": " + cause.getMessage());
    try {
      template.opsForStream().add(dlq, entry);
      ack(template, streamKey, record);
      log.error(
          "비동기 작업 DLQ 격리 — 수동 확인 필요 stream={} id={} dlq={} reason={}",
          streamKey,
          record.getId(),
          dlq,
          cause.getMessage(),
          cause);
    } catch (RuntimeException e) {
      // DLQ 적재조차 실패하면 ACK 하지 않는다. 유실보다 중복이 낫다.
      log.error("DLQ 적재 실패 — 메시지를 pending 으로 유지 stream={} id={}", streamKey, record.getId(), e);
    }
  }

  private static String typeOf(Map<String, String> payload) {
    String type = payload.get("type");
    return type == null ? "unknown" : type;
  }

  /**
   * 작업을 처리한다.
   *
   * <p><b>실패하면 예외를 던진다.</b> 예전에는 여기서 모든 예외를 삼켜서, 호출부가 실패를 알 수 없었고 그대로 ACK 되어 메시지가 사라졌다.
   */
  private void handleJob(Map<String, String> payload) {
    String type = payload.get("type");
    if (type == null) {
      return;
    }
    try {
      switch (AsyncJobType.valueOf(type)) {
        case NOTIFICATION -> handleNotification(payload);
        case TRANSACTION_SYNC, TRANSFER ->
            // 페이로드에는 알림 제목·본문(금액·계좌 포함)이 들어 있어 타입만 남긴다.
            log.debug("비동기 작업 수신 type={}", type);
        default -> log.warn("Unknown async job type={}", type);
      }
    } catch (IllegalArgumentException e) {
      // 알 수 없는 타입은 재시도해도 소용없다. 여기서만 삼키고 ACK 되게 둔다.
      log.warn("알 수 없는 비동기 작업 타입 — 폐기 type={}", type);
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
