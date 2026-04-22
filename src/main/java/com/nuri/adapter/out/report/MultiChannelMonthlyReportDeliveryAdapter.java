package com.nuri.adapter.out.report;

import com.nuri.application.port.out.MonthlyReportDeliveryPort;
import com.nuri.config.MonthlyReportDeliveryProperties;
import com.nuri.domain.entity.NotificationEntity;
import com.nuri.domain.entity.UserEntity;
import com.nuri.domain.repository.NotificationRepository;
import com.nuri.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 인앱 알림(큐 적재) + 선택적 웹훅으로 월간 리포트 발송을 처리합니다.
 */
@Slf4j
@Component
public class MultiChannelMonthlyReportDeliveryAdapter implements MonthlyReportDeliveryPort {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MonthlyReportDeliveryProperties deliveryProperties;
    private final RestTemplate restTemplate;

    public MultiChannelMonthlyReportDeliveryAdapter(NotificationRepository notificationRepository, UserRepository userRepository,
                                                    MonthlyReportDeliveryProperties deliveryProperties, RestTemplate restTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.deliveryProperties = deliveryProperties;
        this.restTemplate = restTemplate;
    }

    @Override
    public void deliver(String userId, byte[] pdfBytes, String fileName) {
        log.info("Monthly report delivery user={} file={} size={}", userId, fileName, pdfBytes.length);

        UUID uuid = parseUuid(userId);
        if (uuid != null) {
            UserEntity user = userRepository.findById(uuid).orElse(null);
            if (user != null) {
                NotificationEntity n = new NotificationEntity();
                n.setRecipientUser(user);
                n.setNotificationType(NotificationEntity.NotificationType.REPORT_READY);
                n.setChannel(NotificationEntity.Channel.PUSH);
                n.setTitle("월간 리포트 준비됨");
                n.setBody("파일: " + fileName + " (" + pdfBytes.length + " bytes)");
                n.setStatus(NotificationEntity.Status.QUEUED);
                n.setSentAt(LocalDateTime.now());
                notificationRepository.save(n);
            }
        }

        if (deliveryProperties.isWebhookEnabled() && deliveryProperties.getWebhookUrl() != null && !deliveryProperties.getWebhookUrl().isBlank()) {
            postWebhook(userId, fileName, pdfBytes);
        }
    }

    private void postWebhook(String userId, String fileName, byte[] pdfBytes) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", userId);
            body.put("fileName", fileName);
            body.put("sizeBytes", pdfBytes.length);
            if (deliveryProperties.isAttachPdfBase64()) {
                body.put("pdfBase64", Base64.getEncoder().encodeToString(pdfBytes));
            }
            restTemplate.postForEntity(
                    deliveryProperties.getWebhookUrl(),
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (RestClientException e) {
            log.warn("Monthly report webhook delivery failed: {}", e.getMessage());
        }
    }

    private static UUID parseUuid(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
