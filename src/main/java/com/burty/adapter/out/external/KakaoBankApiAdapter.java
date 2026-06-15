/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (KakaoBankApiAdapter)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.external
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
package com.burty.adapter.out.external;

import com.burty.application.port.out.bank.KakaoBankPort;
import com.burty.config.ExternalFinanceProperties;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoBankApiAdapter extends AbstractBankTransferAdapter implements KakaoBankPort {

  public KakaoBankApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties) {
    super(restTemplate, properties);
  }

  @Override
  public Map<String, Object> transfer(String userId, String toAccount, long amount) {
    return transfer(
        "KAKAO_BANK",
        properties.getKakaoTransferUrl(),
        properties.getKakaoApiKey(),
        userId,
        toAccount,
        amount);
  }
}
