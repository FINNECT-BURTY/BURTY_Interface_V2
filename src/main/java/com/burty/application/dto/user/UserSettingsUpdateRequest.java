package com.burty.application.dto.user;

import java.math.BigDecimal;

/**
 * 표시 설정 변경.
 *
 * <p>두 값 모두 비워 둘 수 있다. 비운 값은 바꾸지 않는다 — 글자 크기만 바꾸려는 요청이 모드까지 함께 덮어쓰면, 화면에서 건드리지 않은 설정이 조용히 되돌아간다.
 *
 * <p>검증은 서비스에서 한다. 어떤 경로로 들어오든 같은 규칙이 걸려야 한다.
 */
public record UserSettingsUpdateRequest(String uxMode, BigDecimal fontScale) {}
