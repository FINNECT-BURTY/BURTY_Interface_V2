/**
 *
 *
 * <pre>
 * <b>Description  : 코어 (CommonConstants)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.constants
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
package com.burty.core.constants;

public class CommonConstants {

  // API 관련 상수
  public static final String API_VERSION = "v1";
  public static final String API_BASE_PATH = "/api/" + API_VERSION;

  // JWT — 실제 secret 은 burty.jwt.secret (환경변수 JWT_SECRET) 사용
  public static final long JWT_ACCESS_TOKEN_EXPIRATION_TIME = 1800000; // 30분 (밀리초)
  public static final long JWT_REFRESH_TOKEN_EXPIRATION_TIME = 604800000; // 7일 (밀리초)
  public static final String JWT_TOKEN_PREFIX = "Bearer ";
  public static final String JWT_HEADER_NAME = "Authorization";
  public static final String REFRESH_TOKEN_HEADER_NAME = "X-Refresh-Token";

  // 인증 관련 상수
  public static final String ROLE_USER = "ROLE_USER";
  public static final String ROLE_ADMIN = "ROLE_ADMIN";

  // 응답 메시지
  public static final String SUCCESS_MESSAGE = "성공";
  public static final String ERROR_MESSAGE = "오류가 발생했습니다";
  public static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다";
  public static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다";

  // 날짜 포맷
  public static final String DATE_FORMAT = "yyyy-MM-dd";
  public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  // 파일 업로드 한도·허용 확장자는 file.storage.* (FileStorageProperties / FileUtil) 사용

  private CommonConstants() {
    // 유틸리티 클래스이므로 인스턴스화 방지
  }
}
