/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (ChallengeStore)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.out.store
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
package com.burty.adapter.out.store;

public interface ChallengeStore {
  void put(String key, String value, long ttlSeconds);

  String get(String key);

  void remove(String key);

  /**
   * 챌린지를 선점하며 지우고, 실제로 지웠는지 돌려준다.
   *
   * <p>챌린지는 검증 성공 여부와 <b>무관하게</b> 한 번만 쓸 수 있어야 한다. 성공했을 때만 지우면 실패한 시도는 챌린지를 남기고, TTL 이 다할 때까지 같은
   * 챌린지로 몇 번이든 다시 시도할 수 있다.
   *
   * @return {@code true} 면 이 호출이 챌린지를 선점했다, {@code false} 면 없거나 이미 소비됐다
   */
  boolean consume(String key);
}
