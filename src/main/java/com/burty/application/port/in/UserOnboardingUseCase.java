package com.burty.application.port.in;

import com.burty.domain.model.OnboardingProfileResult;

import java.time.LocalDate;

public interface UserOnboardingUseCase {

    OnboardingProfileResult completeProfile(
            String userId,
            String phone,
            String name,
            LocalDate birthDate,
            Integer ageRange,
            String uxModeRaw,
            boolean termsAccepted
    );
}
