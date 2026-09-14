package com.burty.application.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 푸시 등록 토큰.
 *
 * <p>사용자를 식별하는 값이 아니라 <b>기기를 가리키는 주소</b>다. 어느 사용자의 토큰인지는 요청 본문이 아니라 인증 토큰과 경로의 기기로 정한다.
 *
 * <p>길이를 막는 이유: 컬럼이 varchar(255) 라 더 긴 값이 오면 저장 단계에서 잘리거나 실패한다. 잘린 토큰으로는 푸시가 한 건도 가지 않는데 등록은 성공한
 * 것처럼 보이므로, 들어오는 자리에서 거절한다.
 */
public record DeviceFcmTokenRequest(
    @NotBlank(message = "푸시 토큰을 입력해 주세요.") @Size(max = 255, message = "푸시 토큰이 너무 깁니다.")
        String fcmToken) {}
