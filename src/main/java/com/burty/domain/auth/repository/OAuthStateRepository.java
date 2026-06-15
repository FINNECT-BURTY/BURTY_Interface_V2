/**
 *
 *
 * <pre>
 * <b>Description  : 인증 리포지토리 (OAuthStateRepository)</b>
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

import com.burty.domain.auth.entity.OAuthStateEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthStateRepository extends JpaRepository<OAuthStateEntity, String> {

  @Modifying
  @Query("delete from OAuthStateEntity s where s.expiresAt < :now")
  int deleteExpired(@Param("now") LocalDateTime now);
}
