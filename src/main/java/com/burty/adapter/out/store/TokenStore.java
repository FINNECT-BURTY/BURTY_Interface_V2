/**
 *
 *
 * <pre>
 * <b>Description  : 외부연동 (TokenStore)</b>
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

public interface TokenStore {
  void put(String key, String value);

  String get(String key);

  void remove(String key);
}
