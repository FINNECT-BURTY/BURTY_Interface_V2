package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tbl_user_profile")
@Getter
@Setter
public class UserProfileEntity {
    @Id
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "name_encrypted", nullable = false)
    private byte[] nameEncrypted;

    @Column(name = "birthdate_encrypted", nullable = false)
    private byte[] birthdateEncrypted;

    @Column(name = "age_range")
    private Integer ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "ux_mode", nullable = false)
    private UxMode uxMode = UxMode.STANDARD;

    @Column(name = "font_scale", nullable = false, precision = 3, scale = 2)
    private BigDecimal fontScale = BigDecimal.valueOf(1.0);

    @Column(name = "voice_enabled", nullable = false)
    private Boolean voiceEnabled = false;

    @Column(name = "preferences", columnDefinition = "JSON")
    private String preferences;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum UxMode { SENIOR, STANDARD }
}
