/**
 *
 *
 * <pre>
 * <b>Description  : 인증 요청 DTO (ProfileOnboardingRequest)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.auth
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
package com.burty.application.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

/**
 * 소셜 로그인 직후 추가 정보 — {@code UserEntity}(연락처), {@code UserProfileEntity}(실명·생년월일·UX), 화면정의서
 * LGN-006(필수 약관 동의)에 대응합니다.
 */
public record ProfileOnboardingRequest(
    String phone,
    String name,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
    Integer ageRange,
    String uxMode,
    Boolean termsAccepted) {}
