package com.burty.domain.repository;

import com.burty.domain.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PolicyRepository extends JpaRepository<PolicyEntity, String> {

    @Query("""
            select p from PolicyEntity p
            where p.active = true
              and (p.validFrom is null or p.validFrom <= :today)
              and (p.validTo is null or p.validTo >= :today)
              and (p.ageMin is null or p.ageMin <= :age)
              and (p.ageMax is null or p.ageMax >= :age)
              and (p.incomeMax is null or p.incomeMax >= :monthlyIncome)
              and (p.occupationCode is null or p.occupationCode = :occupationCode)
            """)
    List<PolicyEntity> findMatching(
            @Param("today") LocalDate today,
            @Param("age") int age,
            @Param("monthlyIncome") long monthlyIncome,
            @Param("occupationCode") String occupationCode
    );

    List<PolicyEntity> findByActiveTrue();
}
