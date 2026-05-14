package com.berty.domain.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_daily_transfer_usage")
public class DailyTransferUsageEntity {
    @EmbeddedId
    private DailyTransferUsageId id;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "transfer_count", nullable = false)
    private Integer transferCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
