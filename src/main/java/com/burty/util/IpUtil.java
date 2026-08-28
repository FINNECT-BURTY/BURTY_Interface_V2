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
import java.util.List;

/**
 * 클라이언트 IP 조회 진입점.
 *
 * <p>판정 규칙은 {@link ClientIpResolver} 에 있다. 이 클래스는 필터·유틸에서 정적으로 부를 수 있게 하는 얇은 껍데기다.
 *
 * <p>설정({@code burty.security.trusted-proxies})은 스프링 컨텍스트가 뜰 때 {@link #configure(List)} 로 주입된다. 주입
 * 전에는 전달 헤더를 신뢰하지 않는다 — 설정을 못 읽었을 때 헤더를 믿는 쪽으로 기울면 안 된다.
 */
public final class IpUtil {

  private static volatile ClientIpResolver resolver = new ClientIpResolver(List.of());

  private IpUtil() {}

  /** 신뢰 프록시 대역을 적용한다. {@code ClientIpConfiguration} 이 기동 시 한 번 호출한다. */
  public static void configure(List<String> trustedProxyCidrs) {
    resolver = new ClientIpResolver(trustedProxyCidrs);
  }

  public static String getClientIp(HttpServletRequest request) {
    return resolver.resolve(request);
  }

  public static String getClientIpAddress(HttpServletRequest request) {
    return resolver.resolve(request);
  }
}
