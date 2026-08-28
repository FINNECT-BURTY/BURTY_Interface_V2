/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (LoginRiskService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.auth
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.auth;

import com.burty.application.dto.auth.LoginRiskEvaluateRequest;
import com.burty.application.dto.auth.LoginRiskEvaluateResponse;
import com.burty.application.port.in.auth.LoginRiskUseCase;
import com.burty.application.service.support.AuditLogger;
import com.burty.domain.notification.entity.NotificationEntity;
import com.burty.domain.notification.repository.NotificationRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserRepository;
import com.burty.util.AccountNumberHasher;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginRiskService implements LoginRiskUseCase {

  private final DeviceRepository deviceRepository;
  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final AccountNumberHasher accountNumberHasher;
  private final AuditLogger auditLogger;

  @Override
  public LoginRiskEvaluateResponse evaluate(String userId, LoginRiskEvaluateRequest request) {
    // 본문의 userId 는 쓰지 않는다. 예전에는 그것을 그대로 써서, 인증된 사용자가
    // 남의 userId 를 보내 기기 등록 여부를 캐내고 상대의 알림함에 경고를 쌓을 수 있었다.
    Long userKey = Long.parseLong(userId);
    List<String> reasons = new ArrayList<>();
    if (request.deviceFingerprint() != null
        && deviceRepository
            .findByUser_UserIdAndDeviceFingerprintAndRevokedAtIsNull(
                userKey, accountNumberHasher.hash(request.deviceFingerprint()))
            .isEmpty()) {
      reasons.add("NEW_DEVICE");
    }
    int hour = LocalDateTime.now().getHour();
    if (hour >= 23 || hour < 6) {
      reasons.add("UNUSUAL_TIME");
    }
    if (request.region() != null && !request.region().contains("서울")) {
      reasons.add("REGION_OUT_OF_SEOUL");
    }
    String risk = reasons.isEmpty() ? "LOW" : reasons.size() == 1 ? "MEDIUM" : "HIGH";
    if (!reasons.isEmpty()) {
      notify(userKey, risk, reasons);
      auditLogger.log(
          userId,
          "LOGIN_RISK_EVALUATED",
          request.deviceFingerprint(),
          risk,
          String.join(",", reasons));
    }
    return new LoginRiskEvaluateResponse(risk, reasons);
  }

  private void notify(Long userKey, String risk, List<String> reasons) {
    UserEntity user = userRepository.findById(userKey).orElse(null);
    if (user == null) return;
    NotificationEntity entity = new NotificationEntity();
    entity.setRecipientUser(user);
    entity.setNotificationType(NotificationEntity.NotificationType.UNUSUAL_LOGIN);
    entity.setChannel(NotificationEntity.Channel.IN_APP);
    entity.setTitle("비정상 로그인 가능성이 높아요");
    entity.setBody("위험도 " + risk + ": " + String.join(",", reasons));
    entity.setStatus(NotificationEntity.Status.QUEUED);
    notificationRepository.save(entity);
  }
}
