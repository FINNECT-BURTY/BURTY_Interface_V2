/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 도메인 모델 (ActionFeedbackSummary)</b>
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

import java.util.List;

public record ActionFeedbackSummary(
    String userId,
    int totalExecutedActions,
    int acceptedCount,
    int rejectedCount,
    List<String> recentExecutedActions) {}
