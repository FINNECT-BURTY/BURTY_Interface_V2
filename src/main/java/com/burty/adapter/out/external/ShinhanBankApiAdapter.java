/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 외부 연동 어댑터 (ShinhanBankApiAdapter)</b>
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

import com.burty.application.port.out.bank.ShinhanBankPort;
import com.burty.config.ExternalFinanceProperties;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ShinhanBankApiAdapter extends AbstractBankTransferAdapter implements ShinhanBankPort {

  public ShinhanBankApiAdapter(RestTemplate restTemplate, ExternalFinanceProperties properties) {
    super(restTemplate, properties);
  }

  @Override
  public Map<String, Object> transfer(String userId, String toAccount, long amount) {
    return transfer(
        "SHINHAN_BANK",
        properties.getShinhanTransferUrl(),
        properties.getShinhanApiKey(),
        userId,
        toAccount,
        amount);
  }
}
