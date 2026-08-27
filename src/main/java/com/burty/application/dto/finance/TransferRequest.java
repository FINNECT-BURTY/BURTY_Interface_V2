package com.burty.application.dto.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 이체 요청.
 *
 * <p>{@code userId} 는 더 이상 받지 않는다. 사용자 ID 는 인증 컨텍스트에서만 나온다 ({@code @CurrentUserId}). 바디로 받으면 위조 대상이
 * 되고, 그걸 막는 인터셉터는 필드명 관례에 의존해 취약했다.
 */
public record TransferRequest(
    @NotBlank(message = "출금 계좌는 필수입니다") @Size(max = 80) String fromAccount,
    @NotBlank(message = "입금 계좌는 필수입니다") @Size(max = 80) String toAccount,
    @Positive(message = "이체 금액은 0보다 커야 합니다") long amount,
    @Size(max = 255, message = "적요는 255자를 넘을 수 없습니다") String description,
    @NotBlank(message = "생체인증 정보가 필요합니다") String assertionToken,
    @Size(max = 64, message = "멱등키는 64자를 넘을 수 없습니다") String idempotencyKey) {}
