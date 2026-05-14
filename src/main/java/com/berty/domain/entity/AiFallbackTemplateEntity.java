package com.berty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_ai_fallback_template")
@Getter
@Setter
@NoArgsConstructor
public class AiFallbackTemplateEntity {
    @Id
    @Column(name = "template_key", length = 80)
    private String templateKey;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "occupation_code", length = 40)
    private String occupationCode;

    @Column(name = "cause_type", length = 40)
    private String causeType;

    @Column(name = "template_text", length = 1000, nullable = false)
    private String templateText;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
