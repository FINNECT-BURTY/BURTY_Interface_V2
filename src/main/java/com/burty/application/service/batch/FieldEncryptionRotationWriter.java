package com.burty.application.service.batch;

import com.burty.domain.mydata.entity.LinkedInstitutionEntity;
import com.burty.domain.mydata.repository.LinkedInstitutionRepository;
import com.burty.util.FieldEncryptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 행의 재암호화를 독립 트랜잭션으로 수행한다.
 *
 * <p>배치 클래스와 분리한 이유는 트랜잭션 경계 때문이다. 한 행이 실패해도 다른 행은 계속 처리돼야 하는데, 배치 안의 메서드를 자기 호출하면
 * {@code @Transactional} 프록시가 적용되지 않는다.
 */
@Component
public class FieldEncryptionRotationWriter {

  private final LinkedInstitutionRepository repository;
  private final FieldEncryptor fieldEncryptor;

  public FieldEncryptionRotationWriter(
      LinkedInstitutionRepository repository, FieldEncryptor fieldEncryptor) {
    this.repository = repository;
    this.fieldEncryptor = fieldEncryptor;
  }

  /**
   * 구키로 암호화된 토큰을 현재 키로 다시 쓴다.
   *
   * <p>이미 현재 키로 쓰인 값은 건드리지 않는다. 따라서 여러 번 돌려도 안전하다.
   *
   * @return 실제로 재암호화했으면 true
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean rotate(Long linkId) {
    LinkedInstitutionEntity link = repository.findById(linkId).orElse(null);
    if (link == null) {
      return false;
    }

    boolean changed = false;
    if (fieldEncryptor.needsReEncryption(link.getAccessToken())) {
      link.setAccessToken(fieldEncryptor.encrypt(fieldEncryptor.decrypt(link.getAccessToken())));
      changed = true;
    }
    if (fieldEncryptor.needsReEncryption(link.getRefreshToken())) {
      link.setRefreshToken(fieldEncryptor.encrypt(fieldEncryptor.decrypt(link.getRefreshToken())));
      changed = true;
    }

    if (changed) {
      repository.save(link);
    }
    return changed;
  }
}
