/**
 *
 *
 * <pre>
 * <b>Description  : 상담 도메인 모델 (ConsultationResult)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.consult.model
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
package com.burty.domain.consult.model;

import java.util.List;

public record ConsultationResult(
    String summary, String signalColor, List<String> recommendedActions) {}
