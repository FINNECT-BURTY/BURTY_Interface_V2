/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (PersonaHeuristics)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import com.burty.domain.asset.model.AssetSnapshot;
import org.springframework.stereotype.Component;

@Component
public class PersonaHeuristics {

  public long estimateMonthlyIncome(AssetSnapshot snapshot) {
    long spend = Math.round(snapshot.monthlySpend());
    return Math.max(1_000_000L, Math.round(spend * 1.1));
  }

  public int inferAgeFromUserId(String userId) {
    int base = Math.abs(userId.hashCode() % 10);
    return 24 + base;
  }

  /** 페르소나 프로필용 직업 코드 (대문자). */
  public String inferOccupationCode(AssetSnapshot snapshot) {
    double volatility = snapshot.volatilityPercent();
    if (volatility > 20) {
      return "FREELANCER";
    }
    if (snapshot.monthlySpend() < 2_000_000) {
      return "JOB_SEEKER";
    }
    return "NEW_WORKER";
  }

  /** 정책 매칭용 생활 단계 (소문자). */
  public String inferLifeStage(AssetSnapshot snapshot) {
    return switch (inferOccupationCode(snapshot)) {
      case "FREELANCER" -> "freelancer";
      case "JOB_SEEKER" -> "job_seeker";
      default -> "worker";
    };
  }
}
