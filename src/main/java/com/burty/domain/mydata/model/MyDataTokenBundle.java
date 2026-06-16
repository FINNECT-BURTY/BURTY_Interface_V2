package com.burty.domain.mydata.model;

import java.time.LocalDateTime;

/** 마이데이터 OAuth 토큰 묶음. */
public record MyDataTokenBundle(
    String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {}
