/**
 *
 *
 * <pre>
 * <b>Description  : 관리 리포지토리 (AdminUserRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.admin.repository
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
package com.burty.domain.admin.repository;

import com.burty.domain.admin.entity.AdminUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUserEntity, Long> {

  Optional<AdminUserEntity> findByUsername(String username);

  boolean existsByUsername(String username);
}
