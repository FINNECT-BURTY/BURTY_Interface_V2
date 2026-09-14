package com.burty.application.dto.user;

import java.math.BigDecimal;

/**
 * 표시 설정.
 *
 * <p>가입할 때부터 {@code ux_mode} 와 {@code font_scale} 을 저장하고 있었지만 읽을 방법이 없어 값이 늘 기본값에 머물렀다. 시니어를 대상으로
 * 하는 서비스에서 글자 크기를 바꿀 수 없다는 뜻이었다 (#134).
 *
 * @param uxMode SENIOR 또는 STANDARD
 * @param fontScale 글자 배율. 1.00 이 기본
 * @param voiceEnabled 음성 안내 사용 여부
 */
public record UserSettingsResponse(String uxMode, BigDecimal fontScale, boolean voiceEnabled) {}
