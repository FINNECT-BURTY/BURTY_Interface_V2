/**
 *
 *
 * <pre>
 * <b>Description  : 사용자 리포지토리 (UserRepository)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.domain.user.repository
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
package com.burty.domain.user.repository;

import com.burty.domain.user.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByPhoneHash(String phoneHash);
}
