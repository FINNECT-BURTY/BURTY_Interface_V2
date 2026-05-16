package com.burty.adapter.in.web;

import com.burty.core.controller.BaseController;

import com.burty.adapter.in.web.dto.LoginRiskEvaluateRequest;
import com.burty.adapter.in.web.dto.LoginRiskEvaluateResponse;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.NotificationEntity;
import com.burty.domain.entity.UserEntity;
import com.burty.domain.repository.DeviceRepository;
import com.burty.domain.repository.NotificationRepository;
import com.burty.domain.repository.UserRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/security/login-risk")
@Tag(name = "BURTY Login Risk", description = "새 기기/IP/시간대 기반 이상 로그인 평가 API")
public class LoginRiskController extends BaseController {
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public LoginRiskController(DeviceRepository deviceRepository, UserRepository userRepository,
                               NotificationRepository notificationRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/evaluate")
    @AuthLevel(RiskLevel.LEVEL_2)
    public ApiResponse<LoginRiskEvaluateResponse> evaluate(@RequestBody LoginRiskEvaluateRequest request) {
        UUID userUuid = UUID.fromString(request.getUserId());
        List<String> reasons = new ArrayList<>();
        if (request.getDeviceFingerprint() != null && deviceRepository
                .findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(userUuid, sha256(request.getDeviceFingerprint()))
                .isEmpty()) {
            reasons.add("NEW_DEVICE");
        }
        int hour = LocalDateTime.now().getHour();
        if (hour >= 23 || hour < 6) reasons.add("UNUSUAL_TIME");
        if (request.getRegion() != null && !request.getRegion().contains("서울")) reasons.add("REGION_OUT_OF_SEOUL");
        String risk = reasons.isEmpty() ? "LOW" : reasons.size() == 1 ? "MEDIUM" : "HIGH";
        if (!reasons.isEmpty()) notify(userUuid, risk, reasons);
        return ApiResponse.ok(new LoginRiskEvaluateResponse(risk, reasons));
    }

    private void notify(UUID userUuid, String risk, List<String> reasons) {
        UserEntity user = userRepository.findById(userUuid).orElse(null);
        if (user == null) return;
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientUser(user);
        entity.setNotificationType(NotificationEntity.NotificationType.UNUSUAL_LOGIN);
        entity.setChannel(NotificationEntity.Channel.IN_APP);
        entity.setTitle("이상 로그인 가능성이 있어요");
        entity.setBody("위험도 " + risk + ": " + String.join(",", reasons));
        entity.setStatus(NotificationEntity.Status.QUEUED);
        notificationRepository.save(entity);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
