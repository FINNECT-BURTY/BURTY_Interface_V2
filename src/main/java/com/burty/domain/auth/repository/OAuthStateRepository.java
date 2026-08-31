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

  /**
   * state 를 선점하며 삭제하고, 실제로 지운 행 수를 돌려준다.
   *
   * <p>OAuth state 는 <b>한 번만</b> 쓸 수 있어야 콜백 replay 를 막는다. 조회 후 삭제로는 그 보장이 안 된다 — 같은 state 로 두 콜백이
   * 동시에 들어오면 둘 다 조회에 성공해 둘 다 통과한다.
   *
   * @return 1 이면 이 호출이 state 를 선점했다, 0 이면 이미 다른 호출이 소비했다
   */
  @Modifying(clearAutomatically = true)
  @Query("delete from OAuthStateEntity s where s.stateKey = :stateKey")
  int consume(@Param("stateKey") String stateKey);
}
