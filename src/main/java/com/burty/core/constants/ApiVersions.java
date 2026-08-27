package com.burty.core.constants;

/**
 * API 버전 상수.
 *
 * <p>기존에는 {@code "/api/v1"} 문자열이 {@code BurtyWebConfig} 에 하드코딩되어 있었고, v2 를 도입할 경로가 없었다. 컨트롤러 패키지
 * 하나에 prefix 하나가 붙는 구조라 "v1 과 v2 를 동시에 서비스" 하는 것이 불가능했다.
 *
 * <p>지금 구조는 <b>패키지가 버전을 결정한다.</b>
 *
 * <pre>
 *   com.burty.adapter.in.web.**     → /api/v1   (현행)
 *   com.burty.adapter.in.web.v2.**  → /api/v2   (도입 시)
 * </pre>
 *
 * <p>v2 를 만들 때는 {@code adapter.in.web.v2} 패키지에 컨트롤러를 추가하기만 하면 되고, v1 컨트롤러는 그대로 둔 채 병행 운영하다가 폐기 기한이
 * 지나면 패키지째 삭제한다. 이렇게 해두면 "breaking change 를 못 해서 v1 에 계속 필드를 덧붙이는" 상황을 피할 수 있다.
 *
 * <p>응답 헤더 {@code X-API-Version} 으로 어떤 버전이 처리했는지도 알린다. 클라이언트가 의도한 버전을 쓰고 있는지 확인할 수 있어야 한다.
 */
public final class ApiVersions {

  /** 현행 버전. */
  public static final String V1 = "/api/v1";

  /** 다음 버전 (도입 예정). 패키지 {@code adapter.in.web.v2} 에 매핑된다. */
  public static final String V2 = "/api/v2";

  /** 버전을 알리는 응답 헤더. */
  public static final String VERSION_HEADER = "X-API-Version";

  /**
   * 폐기 예정 API 임을 알리는 헤더 (RFC 8594 계열).
   *
   * <p>버전을 올릴 때 구버전 응답에 이 헤더를 붙여, 클라이언트가 로그로 감지할 수 있게 한다. 조용히 끊고 나서 통보하는 것보다 낫다.
   */
  public static final String DEPRECATION_HEADER = "Deprecation";

  public static final String SUNSET_HEADER = "Sunset";

  private ApiVersions() {}
}
