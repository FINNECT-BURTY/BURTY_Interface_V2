package com.burty.application.service.consult;

import com.burty.config.AiProperties;
import com.burty.config.VoiceProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.ConsentRecordEntity;
import com.burty.domain.user.repository.ConsentRecordRepository;
import org.springframework.stereotype.Component;

/**
 * 개인신용정보를 국외로 보내기 전의 동의 확인.
 *
 * <p>AI 상담 프롬프트에는 총자산·월지출·소득 변동성과 페르소나(직업·가구·소득)가 들어간다. 음성은 사용자의 발화가 그대로 나간다. 운영에서는 이 값들이 국외
 * 사업자(OpenAI 등)로 전송되는데, 그에 대한 고지·동의 없이 처리되고 있었다.
 *
 * <p>동의 화면에서 체크했는지가 아니라 <b>동의 기록이 남아 있는지</b>를 본다. 철회한 동의는 없는 것으로 본다.
 *
 * <p>모의 모드에서는 막지 않는다. 어댑터가 HTTP 를 타지 않아 데이터가 밖으로 나가지 않기 때문이다. 개발·시연에서 동의를 요구하면 확인할 수 있는 것은 줄고 지켜지는
 * 것은 없다.
 */
@Component
public class ExternalAiConsentGuard {

  private final ConsentRecordRepository consentRecords;
  private final AiProperties aiProperties;
  private final VoiceProperties voiceProperties;

  public ExternalAiConsentGuard(
      ConsentRecordRepository consentRecords,
      AiProperties aiProperties,
      VoiceProperties voiceProperties) {
    this.consentRecords = consentRecords;
    this.aiProperties = aiProperties;
    this.voiceProperties = voiceProperties;
  }

  /** AI 상담. 실제 모델을 부를 때만 동의를 요구한다. */
  public void requireConsentForAi(String userId) {
    require(userId, !aiProperties.isStubMode());
  }

  /** 음성 인식·합성. 실제 제공자를 부를 때만 동의를 요구한다. */
  public void requireConsentForVoice(String userId) {
    require(userId, !voiceProperties.isStubMode());
  }

  private void require(String userId, boolean leavesTheCountry) {
    if (!leavesTheCountry) {
      return;
    }
    Long userKey = parseUserKey(userId);
    // 사용자 행이 없는 세션(데모)은 동의를 남길 곳이 없다. 동의가 없으면 보내지 않는다.
    if (userKey == null
        || !consentRecords.existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
            userKey, ConsentRecordEntity.ConsentType.THIRD_PARTY_SHARE)) {
      throw new BusinessException(ErrorCode.OVERSEAS_TRANSFER_CONSENT_REQUIRED);
    }
  }

  private static Long parseUserKey(String userId) {
    try {
      return userId == null ? null : Long.parseLong(userId);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
