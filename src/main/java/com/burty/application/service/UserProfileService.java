package com.burty.application.service;

import com.burty.application.port.in.UserProfileUseCase;
import com.burty.domain.entity.UserProfileEntity;
import com.burty.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserProfileUseCase {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserName(Long userId) {
        return userProfileRepository.findById(userId)
                .map(UserProfileEntity::getName)
                .orElse(null);
    }
}
