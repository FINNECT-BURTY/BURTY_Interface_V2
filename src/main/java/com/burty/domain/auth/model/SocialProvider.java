/**
 *
 *
 * <pre>
 * <b>Description  : 인증 도메인 모델 (SocialProvider)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.model
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
package com.burty.domain.auth.model;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;

public enum SocialProvider {
  KAKAO,
  GOOGLE,
  NAVER,
  APPLE;

  public static SocialProvider parse(String raw) {
    if (raw == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "provider가 필요합니다.");
    }
    try {
      return SocialProvider.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(
          ErrorCode.INVALID_INPUT_VALUE, "provider는 GOOGLE, KAKAO, NAVER, APPLE 중 하나여야 합니다.");
    }
  }
}
