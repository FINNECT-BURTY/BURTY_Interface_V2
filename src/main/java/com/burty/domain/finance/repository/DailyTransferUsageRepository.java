package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.DailyTransferUsageEntity;
import com.burty.domain.finance.entity.DailyTransferUsageId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTransferUsageRepository
    extends JpaRepository<DailyTransferUsageEntity, DailyTransferUsageId> {

  Optional<DailyTransferUsageEntity> findById_UserIdAndId_UsageDate(
      Long userId, java.time.LocalDate usageDate);
}
