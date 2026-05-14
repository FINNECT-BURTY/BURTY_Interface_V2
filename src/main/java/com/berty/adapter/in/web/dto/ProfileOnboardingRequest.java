package com.berty.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * 소셜 로그인 직후 추가 정보 — {@code UserEntity}(연락처), {@code UserProfileEntity}(실명·생년월일·UX),
 * 화면정의서 LGN-006(필수 약관 동의)에 대응합니다.
 */
public class ProfileOnboardingRequest {

    private String phone;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Integer ageRange;
    private String uxMode;
    private Boolean termsAccepted;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(Integer ageRange) {
        this.ageRange = ageRange;
    }

    public String getUxMode() {
        return uxMode;
    }

    public void setUxMode(String uxMode) {
        this.uxMode = uxMode;
    }

    public Boolean getTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(Boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }
}
