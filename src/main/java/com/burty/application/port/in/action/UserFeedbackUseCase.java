/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 유스케이스 포트 (UserFeedbackUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.action
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
package com.burty.application.port.in.action;

import com.burty.application.dto.user.UserFeedbackRequest;

public interface UserFeedbackUseCase {

  /**
   * @param userId 인증 토큰에서 꺼낸 사용자. 요청 본문의 userId 는 신뢰하지 않는다.
   */
  void submit(String userId, UserFeedbackRequest request);
}
