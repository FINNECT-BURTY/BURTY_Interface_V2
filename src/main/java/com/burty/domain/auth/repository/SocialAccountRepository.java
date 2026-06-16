/**
 *
 *
 * <pre>
 * <b>Description  : 인증 리포지토리 (SocialAccountRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.auth.repository
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
package com.burty.domain.auth.repository;

import com.burty.domain.auth.entity.SocialAccountEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {
  Optional<SocialAccountEntity> findByProviderAndProviderUserIdHash(
      String provider, String providerUserIdHash);

  List<SocialAccountEntity> findByUserId(Long userId);

  Optional<SocialAccountEntity> findByUserIdAndProvider(Long userId, String provider);
}
