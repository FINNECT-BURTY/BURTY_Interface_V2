package com.burty.application.service;

import com.burty.application.port.in.YouthPolicyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class YouthPolicySyncBatch {

    private static final Logger log = LoggerFactory.getLogger(YouthPolicySyncBatch.class);

    private final YouthPolicyUseCase youthPolicyUseCase;

    public YouthPolicySyncBatch(YouthPolicyUseCase youthPolicyUseCase) {
        this.youthPolicyUseCase = youthPolicyUseCase;
    }

    @Scheduled(cron = "${burty.youth-center.sync-cron:0 0 3 * * *}")
    public void sync() {
        log.info("[YouthPolicySync] 자동 동기화 시작");
        try {
            int count = youthPolicyUseCase.syncPolicies(null, null, null);
            log.info("[YouthPolicySync] 자동 동기화 완료 — syncedCount={}", count);
        } catch (Exception e) {
            log.error("[YouthPolicySync] 자동 동기화 실패: {}", e.getMessage(), e);
        }
    }
}
