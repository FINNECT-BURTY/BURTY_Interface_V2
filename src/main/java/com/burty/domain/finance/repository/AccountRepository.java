/**
 *
 *
 * <pre>
 * <b>Description  : 금융 리포지토리 (AccountRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.finance.repository
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
package com.burty.domain.finance.repository;

import com.burty.domain.finance.entity.AccountEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
  List<AccountEntity> findByLinkedInstitution_LinkId(Long linkId);

  java.util.Optional<AccountEntity> findByLinkedInstitution_LinkIdAndAccountNoHash(
      Long linkId, String accountNoHash);
}
