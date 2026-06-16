/**
 *
 *
 * <pre>
 * <b>Description  : 정책 애플리케이션 서비스 (PolicyMatchingService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.policy
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
package com.burty.application.service.policy;

import com.burty.application.port.in.policy.PolicyMatchUseCase;
import com.burty.application.port.out.mydata.MyDataPort;
import com.burty.application.service.support.AuditLogger;
import com.burty.core.constant.LogMessages;
import com.burty.domain.asset.model.AssetSnapshot;
import com.burty.domain.policy.entity.PolicyEntity;
import com.burty.domain.policy.entity.PolicyMatchLogEntity;
import com.burty.domain.policy.model.PolicyMatch;
import com.burty.domain.policy.repository.PolicyMatchLogRepository;
import com.burty.domain.policy.repository.PolicyRepository;
import com.burty.util.PersonaHeuristics;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PolicyMatchingService implements PolicyMatchUseCase {
  private static final Logger log = LoggerFactory.getLogger(PolicyMatchingService.class);

  private static final Map<String, String> LIFE_STAGE_TO_OCCUPATION =
      Map.of(
          "worker", "NEW_WORKER",
          "new_worker", "NEW_WORKER",
          "freelancer", "FREELANCER",
          "job_seeker", "JOB_SEEKER",
          "student", "STUDENT",
          "part_timer", "PART_TIMER");

  private final PolicyRepository policyRepository;
  private final PolicyMatchLogRepository matchLogRepository;
  private final MyDataPort myDataPort;
  private final AuditLogger auditLogger;
  private final PersonaHeuristics personaHeuristics;

  public PolicyMatchingService(
      PolicyRepository policyRepository,
      PolicyMatchLogRepository matchLogRepository,
      MyDataPort myDataPort,
      AuditLogger auditLogger,
      PersonaHeuristics personaHeuristics) {
    this.policyRepository = policyRepository;
    this.matchLogRepository = matchLogRepository;
    this.myDataPort = myDataPort;
    this.auditLogger = auditLogger;
    this.personaHeuristics = personaHeuristics;
  }

  @Override
  public List<PolicyMatch> matchForUser(String userId) {
    AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
    int age = personaHeuristics.inferAgeFromUserId(userId);
    long monthlyIncome = personaHeuristics.estimateMonthlyIncome(snapshot);
    String lifeStage = personaHeuristics.inferLifeStage(snapshot);
    List<PolicyMatch> matches = matchPolicies(userId, age, monthlyIncome, lifeStage);
    log.info(LogMessages.Policy.MATCH_KPI, userId, matches.size());
    return matches;
  }

  @Override
  public void applyPolicy(String userId, String policyCode) {
    markApplied(userId, policyCode);
    auditLogger.logSuccess(userId, "POLICY_APPLY", policyCode, "policyCode=" + policyCode);
  }

  @Override
  public List<PolicyMatch> matchPolicies(int age, long monthlyIncome, String lifeStage) {
    return matchPolicies(null, age, monthlyIncome, lifeStage);
  }

  public List<PolicyMatch> matchPolicies(
      String userId, int age, long monthlyIncome, String lifeStage) {
    String occupationCode =
        LIFE_STAGE_TO_OCCUPATION.getOrDefault(
            lifeStage == null ? "" : lifeStage.toLowerCase(), null);
    List<PolicyEntity> candidates =
        policyRepository.findMatching(LocalDate.now(), age, monthlyIncome, occupationCode);
    Map<String, Integer> applyRateBoost = computeApplyRateBoost(candidates);
    List<PolicyMatch> matches =
        candidates.stream()
            .map(
                p ->
                    new PolicyMatch(
                        p.getPolicyCode(),
                        p.getTitle(),
                        p.getSupportType() == null ? "기타" : p.getSupportType(),
                        buildReason(p, lifeStage),
                        calculatePriority(p, monthlyIncome, occupationCode)
                            + applyRateBoost.getOrDefault(p.getPolicyCode(), 0)))
            .sorted(Comparator.comparingInt(PolicyMatch::priorityScore).reversed())
            .limit(3)
            .toList();
    if (userId != null && !matches.isEmpty()) {
      logMatches(userId, matches, occupationCode);
    }
    return matches;
  }

  /**
   * Bandit-style boost: policies with higher historical apply rate get a small score boost (cap
   * +15). Acts as the "exploit" signal alongside base priority's "explore" signal.
   */
  private Map<String, Integer> computeApplyRateBoost(List<PolicyEntity> candidates) {
    Map<String, Integer> boost = new HashMap<>();
    for (PolicyEntity p : candidates) {
      long matched = matchLogRepository.countByPolicyCode(p.getPolicyCode());
      if (matched < 10) {
        boost.put(p.getPolicyCode(), 0);
        continue;
      }
      long applied = matchLogRepository.countByPolicyCodeAndAppliedTrue(p.getPolicyCode());
      double rate = applied / (double) matched;
      boost.put(p.getPolicyCode(), (int) Math.min(15.0, rate * 30.0));
    }
    return boost;
  }

  @Override
  public void markApplied(String userId, String policyCode) {
    matchLogRepository
        .findFirstByUserIdAndPolicyCodeOrderByMatchedAtDesc(userId, policyCode)
        .ifPresent(
            entry -> {
              entry.setApplied(true);
              entry.setAppliedAt(java.time.LocalDateTime.now());
              matchLogRepository.save(entry);
            });
  }

  private void logMatches(String userId, List<PolicyMatch> matches, String occupationCode) {
    try {
      List<PolicyMatchLogEntity> logs = new ArrayList<>(matches.size());
      for (int i = 0; i < matches.size(); i++) {
        PolicyMatch m = matches.get(i);
        PolicyMatchLogEntity entry = new PolicyMatchLogEntity();
        entry.setUserId(userId);
        entry.setPolicyCode(m.policyId());
        entry.setPolicyTitle(m.policyName());
        entry.setPriorityScore(m.priorityScore());
        entry.setRankInMatch(i + 1);
        entry.setOccupationCode(occupationCode);
        logs.add(entry);
      }
      matchLogRepository.saveAll(logs);
    } catch (Exception e) {
      log.warn("Policy match log save failed userId={} err={}", userId, e.getMessage());
    }
  }

  private int calculatePriority(PolicyEntity policy, long monthlyIncome, String occupationCode) {
    int score = policy.getPriorityBase() == null ? 50 : policy.getPriorityBase();
    if (policy.getIncomeMax() != null) {
      long gap = Math.max(0, policy.getIncomeMax() - monthlyIncome);
      score += (int) Math.min(30, gap / 100_000);
    }
    if (policy.getOccupationCode() != null
        && policy.getOccupationCode().equalsIgnoreCase(occupationCode)) {
      score += 20;
    }
    return score;
  }

  private String buildReason(PolicyEntity policy, String lifeStage) {
    StringBuilder sb = new StringBuilder("연령/소득 조건 충족");
    if (policy.getOccupationCode() != null) {
      sb.append(", 현재 생활 유형(").append(lifeStage).append(")과 정책 대상이 일치");
    }
    sb.append("합니다.");
    return sb.toString();
  }
}
