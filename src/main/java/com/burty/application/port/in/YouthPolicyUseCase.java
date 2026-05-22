package com.burty.application.port.in;

import com.burty.domain.entity.YouthPolicyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface YouthPolicyUseCase {

    int syncPolicies(String zipCd, String lclsfNm, String keyword);

    Page<YouthPolicyEntity> searchPolicies(
            String lclsfNm,
            String mclsfNm,
            String zipCd,
            String keyword,
            Integer minAge,
            Integer maxAge,
            Pageable pageable
    );
}
