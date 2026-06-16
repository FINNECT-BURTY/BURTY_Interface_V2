/**
 *
 *
 * <pre>
 * <b>Description  : 금융 애플리케이션 서비스 (RegisteredAccountService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.finance
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
package com.burty.application.service.finance;

import com.burty.application.port.in.finance.RegisteredAccountUseCase;
import com.burty.domain.finance.entity.RegisteredAccountEntity;
import com.burty.domain.finance.repository.RegisteredAccountRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisteredAccountService implements RegisteredAccountUseCase {

  private final RegisteredAccountRepository repository;

  public RegisteredAccountService(RegisteredAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public RegisteredAccountEntity register(String userId, String accountNo, String alias) {
    String pk = userId + "|" + accountNo;
    RegisteredAccountEntity entity =
        repository.findById(pk).orElseGet(RegisteredAccountEntity::new);
    entity.setRegisteredId(pk);
    entity.setUserId(userId);
    entity.setAccountNo(accountNo);
    if (alias != null && !alias.isBlank()) {
      entity.setAlias(alias);
    }
    return repository.save(entity);
  }

  @Override
  @Transactional
  public boolean unregister(String userId, String accountNo) {
    String pk = userId + "|" + accountNo;
    if (!repository.existsById(pk)) {
      return false;
    }
    repository.deleteById(pk);
    return true;
  }

  @Override
  public List<View> list(String userId) {
    return repository.findByUserId(userId).stream()
        .map(e -> new View(e.getAccountNo(), e.getAlias()))
        .toList();
  }
}
