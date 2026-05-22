package com.burty.application.port.out;

import com.burty.domain.entity.YouthPolicyEntity;

import java.util.List;

public interface YouthPolicyPort {

    List<YouthPolicyEntity> fetchPolicies(int pageNum, int pageSize, String zipCd, String lclsfNm, String keyword);
}
