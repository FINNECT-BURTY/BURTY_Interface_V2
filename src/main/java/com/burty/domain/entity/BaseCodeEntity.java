package com.burty.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_code",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_code_group_value", columnNames = {"code_group", "code_value"})
        },
        indexes = {
                @Index(name = "idx_code_parent", columnList = "parent_code_id"),
                @Index(name = "idx_code_group_use_sort", columnList = "code_group, use_yn, sort_order")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BaseCodeEntity {

    @Id
    @Column(name = "code_id", length = 64, nullable = false)
    private String codeId;

    @Column(name = "code_group", length = 40, nullable = false)
    private String codeGroup;

    @Column(name = "code_value", length = 40, nullable = false)
    private String codeValue;

    @Column(name = "code_name_ko", length = 80, nullable = false)
    private String codeNameKo;

    @Column(name = "code_name_en", length = 80)
    private String codeNameEn;

    @Column(name = "parent_code_id", length = 64)
    private String parentCodeId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "attr1", length = 255)
    private String attr1;

    @Column(name = "attr2", length = 255)
    private String attr2;

    @Column(name = "attr3", length = 255)
    private String attr3;

    @Column(name = "attr4", length = 255)
    private String attr4;

    @Column(name = "attr5", length = 255)
    private String attr5;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}