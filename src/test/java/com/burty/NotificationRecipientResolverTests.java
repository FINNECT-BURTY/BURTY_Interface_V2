package com.burty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.burty.application.service.notification.NotificationRecipientResolver;
import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.user.entity.DeviceEntity;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 푸시 수신 토큰.
 *
 * <p>예전에는 기기 신뢰 토큰을 푸시 대상으로 돌려줬다. FCM 이 모르는 값이라 푸시가 한 건도 가지 않았고, 인증 수단인 기기 신뢰 토큰이 외부 제공자에게 요청 본문으로
 * 나갔다 (#156).
 */
class NotificationRecipientResolverTests {

  private static final String USER_ID = "7";

  @Test
  @DisplayName("푸시는 FCM 등록 토큰으로만 보낸다 — FCM 토큰이 없는 기기는 대상이 아니다")
  void pushUsesFcmTokenOnly() {
    DeviceEntity registered = new DeviceEntity();
    registered.setFcmToken("fcm-registered");
    registered.setDeviceTokenHash("hash-a");
    DeviceEntity trustedOnly = new DeviceEntity();
    trustedOnly.setDeviceTokenHash("hash-b");

    DeviceRepository devices = mock(DeviceRepository.class);
    when(devices.findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(7L))
        .thenReturn(List.of(registered, trustedOnly));
    NotificationRecipientResolver resolver =
        new NotificationRecipientResolver(
            mock(UserRepository.class), mock(SocialAccountRepository.class), devices);

    assertEquals(List.of("fcm-registered"), resolver.resolvePushTokens(USER_ID));
  }

  @Test
  @DisplayName("빈 FCM 토큰은 보내지 않는다")
  void blankFcmTokenIsSkipped() {
    DeviceEntity blank = new DeviceEntity();
    blank.setFcmToken("  ");

    DeviceRepository devices = mock(DeviceRepository.class);
    when(devices.findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(7L))
        .thenReturn(List.of(blank));
    NotificationRecipientResolver resolver =
        new NotificationRecipientResolver(
            mock(UserRepository.class), mock(SocialAccountRepository.class), devices);

    assertEquals(List.of(), resolver.resolvePushTokens(USER_ID));
  }
}
