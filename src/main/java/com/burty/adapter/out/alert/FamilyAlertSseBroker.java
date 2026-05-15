package com.burty.adapter.out.alert;

import com.burty.domain.model.FamilyAlert;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class FamilyAlertSseBroker {
    private static final Logger log = LoggerFactory.getLogger(FamilyAlertSseBroker.class);

    private final long timeoutMs;
    private final long heartbeatSeconds;
    private final Map<String, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public FamilyAlertSseBroker(
            @Value("${burty.sse.timeout-ms:600000}") long timeoutMs,
            @Value("${burty.sse.heartbeat-seconds:30}") long heartbeatSeconds) {
        this.timeoutMs = timeoutMs;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    @PostConstruct
    void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emittersByUser.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("userId", userId, "ts", System.currentTimeMillis())));
        } catch (IOException ignored) {
            remove(userId, emitter);
        }
        return emitter;
    }

    public void publish(FamilyAlert alert) {
        List<SseEmitter> emitters = emittersByUser.get(alert.getUserId());
        if (emitters == null || emitters.isEmpty()) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("family-alert").data(alert));
            } catch (IOException e) {
                remove(alert.getUserId(), emitter);
            }
        }
    }

    private void sendHeartbeat() {
        long ts = System.currentTimeMillis();
        emittersByUser.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(ts).reconnectTime(3_000));
                } catch (Exception e) {
                    log.debug("SSE heartbeat dropped userId={} reason={}", userId, e.getMessage());
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) emittersByUser.remove(userId);
    }
}
