package com.burty.application.service.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.burty.adapter.out.queue.RedisStreamAsyncJobAdapter;
import com.burty.application.port.out.notify.NotificationChannelPort;
import com.burty.application.port.out.queue.AsyncJobPort;
import com.burty.application.port.out.queue.AsyncJobType;
import com.burty.application.service.notification.NotificationDispatcher;
import com.burty.config.NotifyProperties;
import com.burty.config.QueueProperties;
import com.burty.support.RedisTestContainer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 비동기 작업 큐의 Redis Stream 왕복.
 *
 * <p>이 경로는 {@code burty.redis.enabled=true} 에서만 살아나므로 지금까지 한 번도 실행된 적이 없었다. 그런데 여기에 담긴 규칙은 전부 <b>유실
 * 방지</b>에 관한 것이다.
 *
 * <ul>
 *   <li>성공했을 때만 ACK 한다 — 예전에는 결과와 무관하게 항상 ACK 해서 실패한 알림이 영구 유실됐다
 *   <li>실패하면 pending 으로 남아 재전달된다
 *   <li>전달 횟수가 임계치를 넘으면 DLQ 로 옮기고 ACK 한다 (무한 재시도 방지)
 *   <li>죽은 컨슈머가 물고 있던 메시지를 회수한다
 * </ul>
 *
 * <p>규칙을 코드로만 읽어서는 지켜지는지 알 수 없다. 진짜 Redis 에 걸어 확인한다. Docker 가 없으면 건너뛴다.
 */
class AsyncJobQueueContractTests {

  private static final String USER = "1001";

  private StringRedisTemplate redis;
  private QueueProperties queueProperties;
  private AsyncJobPort publisher;
  private AsyncJobConsumer consumer;
  private RecordingChannel channel;

  @BeforeEach
  void setUp() {
    redis = RedisTestContainer.template();
    assumeTrue(redis != null, "Docker 가 없어 Redis 큐 검증을 건너뛴다");

    // 테스트마다 스트림을 격리한다. 남은 pending 이 다음 테스트를 오염시키면 원인을 못 찾는다.
    queueProperties = new QueueProperties();
    queueProperties.setStreamPrefix("test:" + UUID.randomUUID() + ":");
    queueProperties.setEnabled(true);
    queueProperties.setMaxDeliveries(3);
    queueProperties.setClaimMinIdleMs(0L);

    channel = new RecordingChannel();
    NotificationDispatcher dispatcher =
        new NotificationDispatcher(List.of(channel), null, new Absent<>(), new NotifyProperties());

    publisher = new RedisStreamAsyncJobAdapter(redis, queueProperties);
    consumer =
        new AsyncJobConsumer(queueProperties, dispatcher, new Absent<>(), new Present<>(redis));
    consumer.initRedisConsumerGroups();
  }

  @Test
  @DisplayName("발행한 작업을 소비하고 성공하면 ACK 한다")
  void consumesAndAcknowledgesOnSuccess() {
    publishNotification();

    consumer.pollJobs();

    assertEquals(1, channel.sent.size(), "알림이 전달되지 않았다");
    assertEquals(0, pendingCount(), "성공했는데 pending 으로 남았다");
  }

  @Test
  @DisplayName("처리에 실패하면 ACK 하지 않고 pending 으로 남긴다")
  void keepsPendingOnFailure() {
    channel.failing.set(true);
    publishNotification();

    consumer.pollJobs();

    // ACK 해버리면 실패한 알림이 영구 유실된다. 예전 구현의 결정적 문제였다.
    assertEquals(1, pendingCount(), "실패했는데 ACK 됐다 — 메시지가 유실된다");
    assertEquals(0, deadLetterCount(), "첫 실패에 바로 DLQ 로 갔다");
  }

  @Test
  @DisplayName("전달 횟수가 임계치를 넘으면 DLQ 로 옮기고 ACK 한다")
  void movesToDeadLetterAfterMaxDeliveries() {
    channel.failing.set(true);
    publishNotification();

    // maxDeliveries=3. 재전달을 반복하면 결국 격리돼야 한다. 안 그러면 이 메시지가
    // 큐 앞을 영원히 막는다.
    for (int i = 0; i < 5 && deadLetterCount() == 0; i++) {
      consumer.pollJobs();
    }

    assertEquals(1, deadLetterCount(), "임계치를 넘겼는데 DLQ 로 격리되지 않았다");
    assertEquals(0, pendingCount(), "DLQ 로 옮긴 뒤 ACK 하지 않았다 — 계속 재전달된다");
  }

  @Test
  @DisplayName("DLQ 항목은 원본 위치와 실패 원인을 담는다")
  void deadLetterCarriesDiagnostics() {
    channel.failing.set(true);
    publishNotification();
    for (int i = 0; i < 5 && deadLetterCount() == 0; i++) {
      consumer.pollJobs();
    }

    MapRecord<String, Object, Object> entry = deadLetterRecords().get(0);
    Map<Object, Object> value = entry.getValue();
    // 격리된 메시지는 사람이 확인해야 한다. 어디서 왔고 왜 실패했는지가 없으면 확인할 수 없다.
    assertNotNull(value.get("_originalStream"), "원본 스트림이 없다");
    assertNotNull(value.get("_originalId"), "원본 ID 가 없다");
    assertTrue(String.valueOf(value.get("_error")).contains("NotificationDeliveryException"));
    assertEquals(USER, value.get("userId"));
  }

  @Test
  @DisplayName("다른 컨슈머가 물고 있던 pending 메시지를 회수해 처리한다")
  void reclaimsStalledMessages() {
    publishNotification();

    // 죽은 컨슈머를 흉내낸다: 다른 이름으로 읽어 pending 에 남긴 뒤 그대로 사라진다.
    redis
        .opsForStream()
        .read(
            org.springframework.data.redis.connection.stream.Consumer.from(
                queueProperties.getConsumerGroup(), "dead-worker"),
            StreamOffset.create(streamKey(), ReadOffset.lastConsumed()));
    assertEquals(1, pendingCount(), "사전 조건이 성립하지 않았다");

    consumer.pollJobs();

    // 회수하지 않으면 그 메시지는 죽은 컨슈머 이름에 영원히 묶인다.
    assertEquals(1, channel.sent.size(), "정체된 메시지를 회수하지 못했다");
    assertEquals(0, pendingCount(), "회수 후 ACK 되지 않았다");
  }

  // ── 도우미 ────────────────────────────────────────────────────────────────

  private void publishNotification() {
    publisher.publish(
        AsyncJobType.NOTIFICATION,
        Map.of("userId", USER, "channel", "PUSH", "title", "제목", "body", "본문"));
  }

  private String streamKey() {
    return queueProperties.streamKey(AsyncJobType.NOTIFICATION.name());
  }

  private long pendingCount() {
    PendingMessages pending =
        redis
            .opsForStream()
            .pending(streamKey(), queueProperties.getConsumerGroup(), Range.unbounded(), 100L);
    return pending == null ? 0 : pending.size();
  }

  private List<MapRecord<String, Object, Object>> deadLetterRecords() {
    List<MapRecord<String, Object, Object>> records =
        redis
            .opsForStream()
            .range(
                queueProperties.deadLetterKey(AsyncJobType.NOTIFICATION.name()), Range.unbounded());
    return records == null ? List.of() : records;
  }

  private int deadLetterCount() {
    return deadLetterRecords().size();
  }

  /** 성공·실패를 조절할 수 있는 알림 채널. */
  private static final class RecordingChannel implements NotificationChannelPort {
    private final List<String> sent = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failing = new AtomicBoolean(false);

    @Override
    public Channel channel() {
      return Channel.PUSH;
    }

    @Override
    public boolean send(String userId, String title, String body) {
      if (failing.get()) {
        return false;
      }
      sent.add(userId);
      return true;
    }
  }

  /** 항상 값을 주는 {@code ObjectProvider}. */
  private record Present<T>(T instance) implements ObjectProvider<T> {
    @Override
    public T getObject() {
      return instance;
    }

    @Override
    public T getObject(Object... args) {
      return instance;
    }

    @Override
    public T getIfAvailable() {
      return instance;
    }

    @Override
    public T getIfUnique() {
      return instance;
    }
  }

  /** 항상 비어 있는 {@code ObjectProvider}. */
  private record Absent<T>() implements ObjectProvider<T> {
    @Override
    public T getObject() {
      throw new IllegalStateException("no bean");
    }

    @Override
    public T getObject(Object... args) {
      throw new IllegalStateException("no bean");
    }

    @Override
    public T getIfAvailable() {
      return null;
    }

    @Override
    public T getIfUnique() {
      return null;
    }
  }
}
