package com.burty;

import com.burty.application.service.consult.ExternalAiConsentGuard;
import com.burty.config.AiProperties;
import com.burty.config.VoiceProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import com.burty.domain.user.entity.ConsentRecordEntity;
import com.burty.domain.user.repository.ConsentRecordRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * 국외 이전 동의 게이트.
 *
 * <p>AI 상담 프롬프트에는 총자산·월지출·페르소나가, 음성에는 사용자의 발화가 들어간다. 운영에서 이 값들은 국외 사업자로 나간다. 동의 기록 없이 나가면 안 된다.
 */
class ExternalAiConsentGuardTests {

  private final ConsentRecordRepository consentRecords =
      Mockito.mock(ConsentRecordRepository.class);

  private ExternalAiConsentGuard guard(boolean aiStub, boolean voiceStub) {
    AiProperties ai = new AiProperties();
    ai.setStubMode(aiStub);
    VoiceProperties voice = new VoiceProperties();
    voice.setStubMode(voiceStub);
    return new ExternalAiConsentGuard(consentRecords, ai, voice);
  }

  @Test
  void 모의_모드에서는_동의가_없어도_막지_않는다() {
    // 어댑터가 HTTP 를 타지 않아 데이터가 밖으로 나가지 않는다. 여기서 동의를 요구하면
    // 개발·시연에서 확인할 수 있는 것만 줄고 지켜지는 것은 없다.
    Mockito.when(
            consentRecords.existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
                ArgumentMatchers.anyLong(), ArgumentMatchers.any()))
        .thenReturn(false);

    Assertions.assertDoesNotThrow(() -> guard(true, true).requireConsentForAi("7"));
    Assertions.assertDoesNotThrow(() -> guard(true, true).requireConsentForVoice("7"));
    Mockito.verify(consentRecords, Mockito.never())
        .existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
            ArgumentMatchers.anyLong(), ArgumentMatchers.any());
  }

  @Test
  void 실제_호출인데_동의_기록이_없으면_보내지_않는다() {
    Mockito.when(
            consentRecords.existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
                7L, ConsentRecordEntity.ConsentType.THIRD_PARTY_SHARE))
        .thenReturn(false);

    BusinessException ai =
        Assertions.assertThrows(
            BusinessException.class, () -> guard(false, true).requireConsentForAi("7"));
    BusinessException voice =
        Assertions.assertThrows(
            BusinessException.class, () -> guard(true, false).requireConsentForVoice("7"));

    Assertions.assertEquals(ErrorCode.OVERSEAS_TRANSFER_CONSENT_REQUIRED, ai.getErrorCode());
    Assertions.assertEquals(ErrorCode.OVERSEAS_TRANSFER_CONSENT_REQUIRED, voice.getErrorCode());
  }

  @Test
  void 동의_기록이_있으면_보낸다() {
    Mockito.when(
            consentRecords.existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
                7L, ConsentRecordEntity.ConsentType.THIRD_PARTY_SHARE))
        .thenReturn(true);

    Assertions.assertDoesNotThrow(() -> guard(false, false).requireConsentForAi("7"));
    Assertions.assertDoesNotThrow(() -> guard(false, false).requireConsentForVoice("7"));
  }

  @Test
  void 철회한_동의는_없는_것으로_본다() {
    // 조회 자체가 revokedAt is null 조건을 건다. 철회하면 이 값이 false 가 되고 그때부터 막힌다.
    Mockito.when(
            consentRecords.existsByUser_UserIdAndConsentTypeAndRevokedAtIsNull(
                7L, ConsentRecordEntity.ConsentType.THIRD_PARTY_SHARE))
        .thenReturn(true, false);

    ExternalAiConsentGuard guard = guard(false, false);
    Assertions.assertDoesNotThrow(() -> guard.requireConsentForAi("7"));
    Assertions.assertThrows(BusinessException.class, () -> guard.requireConsentForAi("7"));
  }

  @Test
  void 사용자를_특정할_수_없으면_보내지_않는다() {
    // 동의를 남길 곳이 없는 세션이다. 기록을 확인할 수 없으면 내보내지 않는 쪽을 택한다.
    ExternalAiConsentGuard guard = guard(false, false);

    Assertions.assertThrows(BusinessException.class, () -> guard.requireConsentForAi("demo-user"));
    Assertions.assertThrows(BusinessException.class, () -> guard.requireConsentForAi(null));
  }
}
