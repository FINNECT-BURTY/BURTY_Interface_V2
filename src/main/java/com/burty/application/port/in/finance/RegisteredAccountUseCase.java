/**
 *
 *
 * <pre>
 * <b>Description  : 금융 유스케이스 포트 (RegisteredAccountUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.finance
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
package com.burty.application.port.in.finance;

import com.burty.domain.finance.entity.RegisteredAccountEntity;
import java.util.List;

public interface RegisteredAccountUseCase {

  RegisteredAccountEntity register(String userId, String accountNo, String alias);

  boolean unregister(String userId, String accountNo);

  List<View> list(String userId);

  record View(String accountNo, String alias) {}
}
