/**
 *
 *
 * <pre>
 * <b>Description  : 거래 (TransactionCategorizer)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.transaction
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
package com.burty.application.service.transaction;

import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.core.code.CodeGroups;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.domain.transaction.entity.CategoryRuleEntity;
import com.burty.domain.transaction.entity.TransactionEntity;
import com.burty.domain.transaction.repository.CategoryRuleRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TransactionCategorizer {
  private static final Logger log = LoggerFactory.getLogger(TransactionCategorizer.class);

  private final CategoryRuleRepository ruleRepository;
  private final BaseCodeUseCase baseCodeUseCase;

  public TransactionCategorizer(
      CategoryRuleRepository ruleRepository, BaseCodeUseCase baseCodeUseCase) {
    this.ruleRepository = ruleRepository;
    this.baseCodeUseCase = baseCodeUseCase;
  }

  public void categorize(TransactionEntity tx) {
    String text = combine(tx.getMerchant(), tx.getMemo()).toLowerCase(Locale.ROOT);
    if (text.isBlank()) return;

    List<MatchResult> matches = new ArrayList<>();
    for (CategoryRuleEntity rule : ruleRepository.findByActiveTrueOrderByPriorityDesc()) {
      if (matchesRule(text, rule)) {
        matches.add(new MatchResult(rule, rule.getPriority()));
      }
    }
    for (BaseCodeEntity kw : baseCodeUseCase.lookup(CodeGroups.MERCHANT_KEYWORD)) {
      if (kw.getAttr1() == null || kw.getCodeNameKo() == null) continue;
      if (text.contains(kw.getCodeNameKo().toLowerCase(Locale.ROOT))
          || (kw.getCodeNameEn() != null
              && text.contains(kw.getCodeNameEn().toLowerCase(Locale.ROOT)))) {
        int prio = parseInt(kw.getAttr2(), 50);
        matches.add(new MatchResult(kw.getAttr1(), prio));
      }
    }

    matches.sort((a, b) -> Integer.compare(b.priority, a.priority));
    if (matches.isEmpty()) return;

    MatchResult winner = matches.get(0);
    if ("OUT".equalsIgnoreCase(tx.getDirection())) {
      if (tx.getExpenseCategoryCode() == null) tx.setExpenseCategoryCode(winner.expenseCategory());
    } else if ("IN".equalsIgnoreCase(tx.getDirection())) {
      if (tx.getIncomeCategoryCode() == null) tx.setIncomeCategoryCode(winner.incomeCategory());
    }
    tx.setCategoryConfidence(confidence(winner.priority));
  }

  private boolean matchesRule(String text, CategoryRuleEntity rule) {
    String pattern =
        rule.getMerchantPattern() == null ? "" : rule.getMerchantPattern().toLowerCase(Locale.ROOT);
    if (pattern.isEmpty()) return false;
    return switch (rule.getMatchType() == null
        ? "CONTAINS"
        : rule.getMatchType().toUpperCase(Locale.ROOT)) {
      case "EXACT" -> text.equals(pattern);
      case "STARTS_WITH" -> text.startsWith(pattern);
      case "REGEX" -> {
        try {
          yield text.matches(pattern);
        } catch (Exception e) {
          yield false;
        }
      }
      default -> text.contains(pattern);
    };
  }

  private double confidence(int priority) {
    return Math.min(1.0, Math.max(0.1, priority / 100.0));
  }

  private String combine(String a, String b) {
    StringBuilder sb = new StringBuilder();
    if (a != null) sb.append(a);
    if (b != null) sb.append(' ').append(b);
    return sb.toString().trim();
  }

  private int parseInt(String s, int fallback) {
    if (s == null) return fallback;
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private record MatchResult(String expenseCategory, String incomeCategory, int priority) {
    MatchResult(CategoryRuleEntity rule, int priority) {
      this(rule.getExpenseCategoryCode(), rule.getIncomeCategoryCode(), priority);
    }

    MatchResult(String expenseCategory, int priority) {
      this(expenseCategory, null, priority);
    }
  }
}
