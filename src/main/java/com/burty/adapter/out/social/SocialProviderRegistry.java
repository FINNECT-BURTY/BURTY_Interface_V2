package com.burty.adapter.out.social;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.model.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spring 이 주입한 모든 SocialProviderStrategy 를 enum 키로 lookup 가능한 map 으로 보관.
 * SocialLoginService 가 provider 별 분기 없이 strategy 를 가져다 쓸 수 있게 함.
 */
@Component
public class SocialProviderRegistry {
    private final Map<SocialProvider, SocialProviderStrategy> strategies;

    public SocialProviderRegistry(List<SocialProviderStrategy> all) {
        Map<SocialProvider, SocialProviderStrategy> map = new EnumMap<>(SocialProvider.class);
        for (SocialProviderStrategy s : all) {
            SocialProviderStrategy prev = map.put(s.supports(), s);
            if (prev != null) {
                throw new IllegalStateException("Duplicate SocialProviderStrategy for " + s.supports());
            }
        }
        this.strategies = map;
    }

    public SocialProviderStrategy get(SocialProvider provider) {
        SocialProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 소셜 로그인 제공자입니다: " + provider);
        }
        return strategy;
    }
}
