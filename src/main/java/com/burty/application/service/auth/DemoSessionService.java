/**
 *
 *
 * <pre>
 * <b>Description  : 인증 애플리케이션 서비스 (DemoSessionService)</b>
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

import com.burty.application.dto.auth.DemoSessionResponse;
import com.burty.application.port.in.auth.DemoSessionUseCase;
import com.burty.domain.user.entity.UserSettingEntity;
import com.burty.domain.user.repository.UserSettingRepository;
import com.burty.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoSessionService implements DemoSessionUseCase {

  private static final String DEMO_USER_ID = "demo-user";

  private final UserSettingRepository userSettingRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public DemoSessionResponse createSession() {
    seedSettings(DEMO_USER_ID);
    return new DemoSessionResponse(
        DEMO_USER_ID,
        jwtTokenProvider.generateToken(DEMO_USER_ID),
        "월말 적자 반복형",
        "월세 D-7 잔액 61만원, 카드값 52만원 결제 예정, 월급일까지 14일 남은 상황",
        "/index.html");
  }

  private void seedSettings(String userId) {
    upsertSetting(userId, "OPENING_BALANCE_OVERRIDE", 610_000L);
    upsertSetting(userId, "SAFETY_BALANCE", 700_000L);
    upsertSetting(userId, "MONTHLY_VARIABLE_BUDGET", 240_000L);
  }

  private void upsertSetting(String userId, String key, long value) {
    UserSettingEntity setting =
        userSettingRepository
            .findByUserIdAndSettingKey(userId, key)
            .orElseGet(UserSettingEntity::new);
    setting.setUserId(userId);
    setting.setSettingKey(key);
    setting.setSettingValueLong(value);
    setting.setSettingValueStr(null);
    userSettingRepository.save(setting);
  }
}
