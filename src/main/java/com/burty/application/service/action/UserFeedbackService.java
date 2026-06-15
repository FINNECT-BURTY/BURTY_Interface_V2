/**
 *
 *
 * <pre>
 * <b>Description  : 행동추천 애플리케이션 서비스 (UserFeedbackService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.action
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
package com.burty.application.service.action;

import com.burty.application.dto.user.UserFeedbackRequest;
import com.burty.application.port.in.action.UserFeedbackUseCase;
import com.burty.domain.action.entity.ActionFeedbackEntity;
import com.burty.domain.action.repository.ActionFeedbackRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserFeedbackService implements UserFeedbackUseCase {

  private final ActionFeedbackRepository actionFeedbackRepository;

  @Override
  public void submit(UserFeedbackRequest request) {
    ActionFeedbackEntity entity = new ActionFeedbackEntity();
    entity.setUserId(request.userId());
    entity.setActionType(
        defaultString(request.targetType(), "GENERAL")
            + ":"
            + defaultString(request.targetId(), "-"));
    entity.setFeedback(
        defaultString(request.feedbackType(), "feedback")
            + "="
            + defaultString(request.feedbackValue(), "-"));
    entity.setCreatedAt(LocalDateTime.now());
    actionFeedbackRepository.save(entity);
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
