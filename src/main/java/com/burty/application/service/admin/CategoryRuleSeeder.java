/**
 *
 *
 * <pre>
 * <b>Description  : 관리 (CategoryRuleSeeder)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.admin
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.15        RosieOh     최초생성
 *        </pre>
 */
package com.burty.application.service.admin;

import com.burty.application.service.transaction.CategoryRuleProvider;
import com.burty.core.constant.LogMessages;
import com.burty.domain.transaction.entity.CategoryRuleEntity;
import com.burty.domain.transaction.repository.CategoryRuleRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategoryRuleSeeder {
  private static final Logger log = LoggerFactory.getLogger(CategoryRuleSeeder.class);

  private final CategoryRuleRepository repository;
  private final CategoryRuleProvider categoryRuleProvider;

  public CategoryRuleSeeder(
      CategoryRuleRepository repository, CategoryRuleProvider categoryRuleProvider) {
    this.repository = repository;
    this.categoryRuleProvider = categoryRuleProvider;
  }

  @PostConstruct
  @Transactional
  public void seed() {
    long inserted = 0;
    inserted += upsert("RULE_RENT", "월세", "CONTAINS", "RENT", 100);
    inserted += upsert("RULE_CARD", "카드", "CONTAINS", "CARD_BILL", 100);
    inserted += upsert("RULE_LOAN", "대출", "CONTAINS", "LOAN", 100);
    inserted += upsert("RULE_KEPCO", "한전", "CONTAINS", "UTIL", 90);
    inserted += upsert("RULE_INSURANCE", "보험", "CONTAINS", "INSURANCE", 85);
    inserted += upsert("RULE_TELECOM_KT", "kt", "CONTAINS", "COMM", 80);
    inserted += upsert("RULE_TELECOM_SKT", "skt", "CONTAINS", "COMM", 80);
    inserted += upsert("RULE_HOSPITAL", "병원", "CONTAINS", "MEDICAL", 85);
    inserted += upsert("RULE_PHARMACY", "약국", "CONTAINS", "MEDICAL", 75);
    inserted += upsert("RULE_NETFLIX", "넷플릭스", "CONTAINS", "ENTERTAIN", 85);
    inserted += upsert("RULE_SUBWAY", "지하철", "CONTAINS", "TRANSPORT", 80);
    inserted += upsert("RULE_BUS", "버스", "CONTAINS", "TRANSPORT", 80);
    log.info(LogMessages.Admin.CATEGORY_RULE_SEEDER, inserted);
    if (inserted > 0) {
      // 규칙이 바뀌었으므로 캐시를 비운다. 안 비우면 TTL 만료까지 옛 규칙으로 분류된다.
      categoryRuleProvider.invalidate();
    }
  }

  private long upsert(
      String id, String pattern, String matchType, String expenseCategoryCode, int priority) {
    if (repository.existsById(id)) return 0;
    CategoryRuleEntity e = new CategoryRuleEntity();
    e.setRuleId(id);
    e.setMerchantPattern(pattern);
    e.setMatchType(matchType);
    e.setExpenseCategoryCode(expenseCategoryCode);
    e.setPriority(priority);
    e.setSource("SYSTEM");
    e.setActive(true);
    LocalDateTime now = LocalDateTime.now();
    e.setCreatedAt(now);
    e.setUpdatedAt(now);
    repository.saveAll(List.of(e));
    return 1;
  }
}
