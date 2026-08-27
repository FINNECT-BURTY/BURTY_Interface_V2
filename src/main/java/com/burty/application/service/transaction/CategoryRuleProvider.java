package com.burty.application.service.transaction;

import com.burty.core.config.CacheConfig;
import com.burty.domain.transaction.entity.CategoryRuleEntity;
import com.burty.domain.transaction.repository.CategoryRuleRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활성 분류 규칙 제공자.
 *
 * <p>규칙은 거의 바뀌지 않는데 <b>거래 한 건마다</b> 조회됐다. 거래내역 동기화 배치가 수천 건을 처리하면 같은 쿼리를 수천 번 날린다. 규칙 변경 시점에만 캐시를
 * 비우면 된다.
 */
@Service
public class CategoryRuleProvider {

  private final CategoryRuleRepository ruleRepository;

  public CategoryRuleProvider(CategoryRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  @Cacheable(CacheConfig.CATEGORY_RULES)
  @Transactional(readOnly = true)
  public List<CategoryRuleEntity> activeRules() {
    return ruleRepository.findByActiveTrueOrderByPriorityDesc();
  }

  /** 규칙을 추가·수정·비활성화한 뒤 반드시 호출한다. */
  @CacheEvict(value = CacheConfig.CATEGORY_RULES, allEntries = true)
  public void invalidate() {
    // 캐시 무효화만 수행한다.
  }
}
