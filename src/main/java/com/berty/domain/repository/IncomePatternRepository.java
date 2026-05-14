package com.berty.domain.repository;

import com.berty.domain.entity.IncomePatternEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomePatternRepository extends JpaRepository<IncomePatternEntity, Long> {
    Optional<IncomePatternEntity> findByUserIdAndPeriodYyyymm(String userId, String periodYyyymm);

    List<IncomePatternEntity> findTop12ByUserIdOrderByPeriodYyyymmDesc(String userId);
}
