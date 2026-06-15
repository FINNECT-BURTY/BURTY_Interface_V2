/**
 *
 *
 * <pre>
 * <b>Description  : 인증 리포지토리 (BiometricCredentialRepository)</b>
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

import com.burty.domain.auth.entity.BiometricCredentialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BiometricCredentialRepository
    extends JpaRepository<BiometricCredentialEntity, Long> {
  Optional<BiometricCredentialEntity> findFirstByUser_UserIdAndRevokedAtIsNull(Long userId);

  List<BiometricCredentialEntity> findByUser_UserIdAndRevokedAtIsNull(Long userId);

  List<BiometricCredentialEntity> findByDevice_DeviceIdAndRevokedAtIsNull(Long deviceId);
}
