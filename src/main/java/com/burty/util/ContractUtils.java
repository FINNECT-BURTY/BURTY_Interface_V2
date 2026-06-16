/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (ContractUtils)</b>
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

/** 금액·전화번호 등 공통 포맷 유틸. */
public final class ContractUtils {

  private ContractUtils() {}

  public static String amtFormatter(Integer amt) {
    if (amt == null) {
      return null;
    }
    return String.format("%,d", amt);
  }

  public static String amtFormatter(Long amt) {
    if (amt == null) {
      return null;
    }
    return String.format("%,d", amt);
  }

  public static int toInt(Integer value) {
    return value != null ? value : 0;
  }

  public static double toDouble(Integer value) {
    return value != null ? value : 0.0;
  }

  public static double toDouble(Double value) {
    return value != null ? value : 0.0;
  }

  public static String formatPhone(String phone) {
    if (phone == null || phone.isBlank()) {
      return phone;
    }
    String digits = phone.replaceAll("\\D", "");
    int len = digits.length();
    if (len < 7) {
      return digits;
    }
    if (len == 7) {
      return digits.substring(0, 3) + "-" + digits.substring(3);
    }
    if (len == 8) {
      return digits.substring(0, 4) + "-" + digits.substring(4);
    }
    if (len == 10) {
      return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
    }
    if (len == 11) {
      return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
    }
    return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
  }
}
