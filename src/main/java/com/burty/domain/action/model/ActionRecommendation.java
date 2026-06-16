/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 도메인 모델 (ActionRecommendation)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.action.model
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
package com.burty.domain.action.model;

public record ActionRecommendation(
    String actionType,
    String title,
    String description,
    long estimatedImprovement,
    double priorityScore,
    String advisoryBoundary) {

  public ActionRecommendation(
      String actionType,
      String title,
      String description,
      long estimatedImprovement,
      double priorityScore) {
    this(
        actionType,
        title,
        description,
        estimatedImprovement,
        priorityScore,
        "조회와 분석 기반의 참고 제안이며, 금융상품 가입/대출 실행/상환 조건 변경은 사용자 확인과 금융기관 절차가 필요합니다.");
  }
}
