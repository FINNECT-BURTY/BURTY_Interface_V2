package com.burty.application.dto.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;

/** 이체 한도 변경 요청. userId 는 인증 컨텍스트에서 가져온다. */
public record LimitUpdateRequest(
    @PositiveOrZero(message = "한도는 0 이상이어야 합니다")
        @Max(value = 1_000_000_000L, message = "한도는 10억원을 넘을 수 없습니다")
        long limit) {}
