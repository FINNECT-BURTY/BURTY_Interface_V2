/**
 *
 *
 * <pre>
 * <b>Description  : 유틸 (IpUtil)</b>
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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** 프록시/nginx 뒤에서 실제 클라이언트 IP 를 추출합니다. */
@Component
public class IpUtil {

  private static final String[] IP_HEADERS = {
    "X-Forwarded-For",
    "X-Real-IP",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_CLIENT_IP",
    "HTTP_X_FORWARDED_FOR"
  };

  public static String getClientIp(HttpServletRequest request) {
    return getClientIpAddress(request);
  }

  public static String getClientIpAddress(HttpServletRequest request) {
    for (String headerName : IP_HEADERS) {
      String ipAddress = request.getHeader(headerName);
      if (isValidIp(ipAddress)) {
        if (ipAddress.contains(",")) {
          ipAddress = ipAddress.split(",")[0].trim();
        }
        if (isValidIpFormat(ipAddress)) {
          return ipAddress;
        }
      }
    }
    String remoteAddr = request.getRemoteAddr();
    return isValidIpFormat(remoteAddr) ? remoteAddr : "unknown";
  }

  private static boolean isValidIp(String ip) {
    return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip) && !"-".equals(ip.trim());
  }

  private static boolean isValidIpFormat(String ip) {
    if (ip == null || ip.isEmpty()) {
      return false;
    }
    if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
      try {
        for (String part : ip.split("\\.")) {
          int num = Integer.parseInt(part);
          if (num < 0 || num > 255) {
            return false;
          }
        }
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    if (ip.contains(":")) {
      return ip.matches("^([0-9a-fA-F]{0,4}:){1,7}[0-9a-fA-F]{0,4}$")
          || "::1".equals(ip)
          || "::".equals(ip);
    }
    return false;
  }
}
