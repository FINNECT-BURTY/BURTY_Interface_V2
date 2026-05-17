package com.burty.domain.repository;

import com.burty.domain.entity.OAuthStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OAuthStateRepository extends JpaRepository<OAuthStateEntity, String> {

    @Modifying
    @Query("delete from OAuthStateEntity s where s.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}