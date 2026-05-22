package com.burty.domain.repository;

import com.burty.domain.entity.YouthPolicyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface YouthPolicyRepository extends JpaRepository<YouthPolicyEntity, Long> {

    Optional<YouthPolicyEntity> findByPlcyNo(String plcyNo);

    @Query("""
            select p from YouthPolicyEntity p
            where (:lclsfNm   is null or p.lclsfNm   = :lclsfNm)
              and (:mclsfNm   is null or p.mclsfNm   = :mclsfNm)
              and (:zipCd     is null or p.zipCd     like %:zipCd%)
              and (:keyword   is null
                   or p.plcyNm     like %:keyword%
                   or p.plcyKywdNm like %:keyword%
                   or p.plcyExplnCn like %:keyword%)
              and (:minAge    is null or p.sprtTrgtMaxAge is null
                   or CAST(p.sprtTrgtMaxAge AS integer) >= :minAge)
              and (:maxAge    is null or p.sprtTrgtMinAge is null
                   or CAST(p.sprtTrgtMinAge AS integer) <= :maxAge)
            """)
    Page<YouthPolicyEntity> search(
            @Param("lclsfNm") String lclsfNm,
            @Param("mclsfNm") String mclsfNm,
            @Param("zipCd") String zipCd,
            @Param("keyword") String keyword,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            Pageable pageable
    );
}
