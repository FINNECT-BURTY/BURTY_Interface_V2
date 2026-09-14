/**
 *
 *
 * <pre>
 * <b>Description  : 인증 유스케이스 포트 (UserOnboardingUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.auth
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
package com.burty.application.port.in.auth;

import com.burty.application.dto.auth.SignupConsents;
import com.burty.domain.auth.model.OnboardingProfileResult;
import java.time.LocalDate;

public interface UserOnboardingUseCase {

  /**
   * 가입 추가 프로필 저장.
   *
   * <p>동의는 항목별로 받아 기록한다. 예전에는 필수 약관 하나(boolean)만 받아 이용약관·개인정보 두 건만 남겼고, 화면에서 받은 수집·이용·전송요구·마케팅·혜택
   * 동의는 기록이 없었다.
   *
   * @param ipAddress 동의 시점의 접속 IP. 기록할 수 없으면 {@code null}
   * @param userAgent 동의 시점의 User-Agent
   */
  OnboardingProfileResult completeProfile(
      String userId,
      String phone,
      String name,
      LocalDate birthDate,
      Integer ageRange,
      String uxModeRaw,
      SignupConsents consents,
      String ipAddress,
      String userAgent);
}
