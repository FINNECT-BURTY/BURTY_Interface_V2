package com.burty.domain.repository;

import com.burty.domain.entity.UserSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSettingEntity, String> {
    Optional<UserSettingEntity> findByUserIdAndSettingKey(String userId, String settingKey);
}
