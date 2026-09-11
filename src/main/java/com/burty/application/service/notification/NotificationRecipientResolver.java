package com.burty.application.service.notification;

import com.burty.domain.auth.repository.SocialAccountRepository;
import com.burty.domain.user.entity.UserEntity;
import com.burty.domain.user.repository.DeviceRepository;
import com.burty.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecipientResolver {

  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final DeviceRepository deviceRepository;

  public NotificationRecipientResolver(
      UserRepository userRepository,
      SocialAccountRepository socialAccountRepository,
      DeviceRepository deviceRepository) {
    this.userRepository = userRepository;
    this.socialAccountRepository = socialAccountRepository;
    this.deviceRepository = deviceRepository;
  }

  public Optional<String> resolvePhone(String userId) {
    return findUser(userId).map(UserEntity::getPhone).filter(p -> p != null && !p.isBlank());
  }

  public Optional<String> resolveEmail(String userId) {
    try {
      Long userKey = Long.parseLong(userId);
      return socialAccountRepository.findByUserId(userKey).stream()
          .map(account -> account.getEmail())
          .filter(email -> email != null && !email.isBlank())
          .findFirst();
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  /**
   * 푸시를 받을 FCM 등록 토큰.
   *
   * <p>기기 신뢰 토큰을 쓰면 안 된다. 예전에는 그것을 돌려줘서 FCM 이 모르는 값으로 보내 푸시가 한 건도 가지 않았고, 인증 수단인 기기 신뢰 토큰이 외부 제공자에게
   * 요청 본문으로 나갔다. FCM 토큰이 없는 기기는 푸시 대상이 아니다.
   */
  public List<String> resolvePushTokens(String userId) {
    try {
      Long userKey = Long.parseLong(userId);
      return deviceRepository
          .findByUser_UserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userKey)
          .stream()
          .map(device -> device.getFcmToken())
          .filter(token -> token != null && !token.isBlank())
          .toList();
    } catch (NumberFormatException e) {
      return List.of();
    }
  }

  private Optional<UserEntity> findUser(String userId) {
    try {
      return userRepository.findById(Long.parseLong(userId));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
