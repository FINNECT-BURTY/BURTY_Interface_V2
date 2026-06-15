/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (CashflowScheduleDateUtils)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.util
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
package com.burty.util;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class CashflowScheduleDateUtils {

  public LocalDate nextOccurrence(LocalDate startDate, int dayOfMonth) {
    int day = Math.max(1, Math.min(28, dayOfMonth));
    LocalDate candidate = startDate.withDayOfMonth(Math.min(day, startDate.lengthOfMonth()));
    if (candidate.isBefore(startDate)) {
      candidate = candidate.plusMonths(1);
      candidate = candidate.withDayOfMonth(Math.min(day, candidate.lengthOfMonth()));
    }
    return candidate;
  }

  public Long parseNumericUserId(String userId) {
    if (userId == null || userId.isBlank()) return null;
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
