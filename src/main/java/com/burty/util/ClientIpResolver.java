package com.burty.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 클라이언트 IP 판정.
 *
 * <p>예전 구현은 {@code X-Forwarded-For}, {@code X-Real-IP}, {@code Proxy-Client-IP} 등 6개 헤더를 <b>아무 검증
 * 없이</b> 읽고, XFF 는 가장 왼쪽 값을 썼다. 두 가지가 다 틀렸다.
 *
 * <ul>
 *   <li>헤더는 클라이언트가 마음대로 넣는다. 신뢰할 프록시를 정해두지 않으면 아무 값이나 "내 IP" 가 된다.
 *   <li>XFF 는 프록시를 지날 때마다 오른쪽에 덧붙는다. 따라서 <b>가장 왼쪽이 클라이언트가 보낸 값</b>이고, 진짜 접속 출처는 오른쪽 끝에 가깝다.
 * </ul>
 *
 * <p>그래서 IP 로 거는 레이트리밋을 헤더 하나로 우회할 수 있었고, 로그인 실패 기록과 로그에는 공격자가 고른 IP 가 남았다.
 *
 * <p>지금은 <b>실제 접속 출처({@code remoteAddr})가 신뢰 프록시일 때만</b> 전달 헤더를 본다. 신뢰 프록시 목록은 {@code
 * burty.security.trusted-proxies} 로 설정한다. 비워두면 헤더를 아예 보지 않는다 — 프록시가 없는 배포에서 안전한 기본값이다.
 *
 * <p>XFF 를 볼 때는 오른쪽부터 훑어 신뢰 프록시가 아닌 첫 항목을 클라이언트로 본다. 공격자가 앞쪽에 가짜 항목을 아무리 넣어도 우리가 세는 위치는 바뀌지 않는다.
 */
public final class ClientIpResolver {

  public static final String UNKNOWN = "unknown";

  /**
   * 전달 헤더가 적용되기 전의 실제 peer 주소.
   *
   * <p>{@code ForwardedHeaderFilter} 는 {@code X-Forwarded-For} 로 {@code remoteAddr} 를 덮어쓴다. 덮어쓴 값을
   * 신뢰 판정의 기준으로 쓰면 클라이언트가 보낸 값을 클라이언트가 보낸 값으로 검증하게 된다. {@code ForwardedHeaderConfiguration} 이 덮어쓰기
   * 전에 이 속성으로 진짜 출처를 남긴다.
   */
  public static final String PEER_ADDRESS_ATTRIBUTE = "burty.peerAddress";

  /**
   * 표준 전달 헤더만 본다.
   *
   * <p>{@code Proxy-Client-IP}, {@code WL-Proxy-Client-IP}, {@code HTTP_CLIENT_IP} 등은 옛
   * WebLogic·PHP 관행이다. 우리 인프라는 쓰지 않으면서 위조 표면만 넓힌다.
   */
  private static final String FORWARDED_FOR = "X-Forwarded-For";

  private static final String REAL_IP = "X-Real-IP";

  private final List<CidrRange> trustedProxies;

  public ClientIpResolver(List<String> trustedProxyCidrs) {
    List<CidrRange> parsed = new ArrayList<>();
    for (String cidr : trustedProxyCidrs) {
      if (cidr != null && !cidr.isBlank()) {
        parsed.add(CidrRange.parse(cidr.trim()));
      }
    }
    this.trustedProxies = List.copyOf(parsed);
  }

  public String resolve(HttpServletRequest request) {
    String remoteAddr = normalize(peerAddress(request));

    // 직접 접속이거나 신뢰하지 않는 출처면 헤더는 무시한다. 그 헤더는 클라이언트가 넣은 것이다.
    if (remoteAddr == null || !isTrustedProxy(remoteAddr)) {
      return remoteAddr == null ? UNKNOWN : remoteAddr;
    }

    String forwarded = request.getHeader(FORWARDED_FOR);
    if (forwarded != null && !forwarded.isBlank()) {
      String[] hops = forwarded.split(",");
      // 오른쪽부터. 신뢰 프록시가 아닌 첫 항목이 클라이언트다.
      for (int i = hops.length - 1; i >= 0; i--) {
        String hop = normalize(hops[i]);
        if (hop != null && !isTrustedProxy(hop)) {
          return hop;
        }
      }
      // 전부 신뢰 프록시였다면 가장 앞이 클라이언트다.
      String first = normalize(hops[0]);
      if (first != null) {
        return first;
      }
    }

    String realIp = normalize(request.getHeader(REAL_IP));
    return realIp != null ? realIp : remoteAddr;
  }

  /** 전달 헤더가 적용되기 전의 출처. 없으면 그대로 {@code remoteAddr}. */
  private static String peerAddress(HttpServletRequest request) {
    Object stashed = request.getAttribute(PEER_ADDRESS_ATTRIBUTE);
    return stashed instanceof String peer ? peer : request.getRemoteAddr();
  }

  private boolean isTrustedProxy(String ip) {
    for (CidrRange range : trustedProxies) {
      if (range.contains(ip)) {
        return true;
      }
    }
    return false;
  }

  /** 형식이 맞는 IP 만 통과시킨다. 로그와 레이트리밋 키에 임의 문자열이 들어가면 안 된다. */
  private static String normalize(String raw) {
    if (raw == null) {
      return null;
    }
    String ip = raw.trim();
    if (ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip) || "-".equals(ip)) {
      return null;
    }
    // IPv6 는 대괄호와 포트가 붙어 올 수 있다: [2001:db8::1]:443
    if (ip.startsWith("[")) {
      int close = ip.indexOf(']');
      if (close > 0) {
        ip = ip.substring(1, close);
      }
    }
    InetAddress parsed = parseLiteral(ip);
    return parsed == null ? null : parsed.getHostAddress().toLowerCase(Locale.ROOT);
  }

  /**
   * 숫자 표기 IP 만 해석한다.
   *
   * <p>{@code InetAddress.getByName} 은 호스트명을 받으면 DNS 를 친다. 헤더 값은 신뢰할 수 없으므로 리터럴 형태를 먼저 확인해 이름 해석
   * 경로로 들어가지 않게 한다. (숫자·점만 있거나 콜론을 포함한 문자열은 호스트명이 될 수 없다.)
   */
  private static InetAddress parseLiteral(String ip) {
    boolean looksNumeric = ip.matches("[0-9.]+") || ip.indexOf(':') >= 0;
    if (!looksNumeric) {
      return null;
    }
    try {
      return InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      return null;
    }
  }

  /** CIDR 한 대역. */
  private record CidrRange(byte[] network, int prefixBits) {

    static CidrRange parse(String cidr) {
      String[] parts = cidr.split("/");
      InetAddress address;
      try {
        address = InetAddress.getByName(parts[0]);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException("신뢰 프록시 설정이 IP 가 아닙니다: " + cidr, e);
      }
      byte[] bytes = address.getAddress();
      int bits = parts.length > 1 ? Integer.parseInt(parts[1]) : bytes.length * 8;
      if (bits < 0 || bits > bytes.length * 8) {
        throw new IllegalArgumentException("신뢰 프록시 prefix 범위를 벗어났습니다: " + cidr);
      }
      return new CidrRange(bytes, bits);
    }

    boolean contains(String ip) {
      InetAddress parsed = parseLiteral(ip);
      if (parsed == null) {
        return false;
      }
      byte[] candidate = parsed.getAddress();
      if (candidate.length != network.length) {
        return false;
      }
      int fullBytes = prefixBits / 8;
      for (int i = 0; i < fullBytes; i++) {
        if (candidate[i] != network[i]) {
          return false;
        }
      }
      int remaining = prefixBits % 8;
      if (remaining == 0) {
        return true;
      }
      int mask = 0xFF << (8 - remaining);
      return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
    }
  }
}
