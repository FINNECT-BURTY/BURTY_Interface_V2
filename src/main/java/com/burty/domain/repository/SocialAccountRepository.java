package com.burty.domain.repository;

import com.burty.domain.entity.SocialAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {
    Optional<SocialAccountEntity> findByProviderAndProviderUserIdHash(String provider, String providerUserIdHash);
    List<SocialAccountEntity> findByUserId(Long userId);
    Optional<SocialAccountEntity> findByUserIdAndProvider(Long userId, String provider);
}
