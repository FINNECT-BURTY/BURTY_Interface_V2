package com.burty.application.service;

import com.burty.application.port.in.UserProfileUseCase;
import com.burty.domain.entity.UserProfileEntity;
import com.burty.domain.repository.SocialAccountRepository;
import com.burty.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserProfileUseCase {

    private final UserProfileRepository userProfileRepository;
    private final SocialAccountRepository socialAccountRepository;

    public UserProfileService(UserProfileRepository userProfileRepository,
                              SocialAccountRepository socialAccountRepository) {
        this.userProfileRepository = userProfileRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserName(Long userId) {
        return userProfileRepository.findById(userId)
                .map(UserProfileEntity::getName)
                .orElseGet(() -> socialAccountRepository.findByUserId(userId).stream()
                        .map(a -> a.getDisplayName())
                        .filter(n -> n != null && !n.isBlank())
                        .findFirst()
                        .orElse(null));
    }
}
