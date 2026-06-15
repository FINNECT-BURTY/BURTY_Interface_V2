/**
 *
 *
 * <pre>
 * <b>Description  : 공통 포트 인터페이스 (EasyReadPort)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.out.ai
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
package com.burty.application.port.out.ai;

public interface EasyReadPort {
  String toEasyRead(String rawText);

  String toSignalColor(double volatilityPercent);
}
