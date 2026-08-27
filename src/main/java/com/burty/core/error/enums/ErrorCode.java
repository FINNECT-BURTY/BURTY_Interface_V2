/**
 *
 *
 * <pre>
 * <b>Description  : 코어 (ErrorCode)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.error.enums
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
package com.burty.core.error.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 공통 에러 (1000번대)
  INVALID_INPUT_VALUE(1000, "잘못된 입력값입니다"),
  METHOD_NOT_ALLOWED(1001, "허용되지 않은 메서드입니다"),
  ENTITY_NOT_FOUND(1002, "요청한 리소스를 찾을 수 없습니다"),
  INTERNAL_SERVER_ERROR(1003, "서버 내부 오류가 발생했습니다"),
  DUPLICATE_RESOURCE(1004, "이미 존재하는 리소스입니다"),
  INVALID_INPUT_PASSWORD(1005, "비밀번호 형식이 맞지 않습니다."),
  JSON_PROCESSING_ERROR(1006, "JSON 처리 중 오류가 발생했습니다"),

  // 인증/인가 에러 (2000번대)
  UNAUTHORIZED(2000, "인증이 필요합니다"),
  FORBIDDEN(2001, "접근 권한이 없습니다"),
  INVALID_TOKEN(2002, "유효하지 않은 토큰입니다"),
  EXPIRED_TOKEN(2003, "만료된 토큰입니다"),
  INVALID_CREDENTIALS(2004, "잘못된 인증 정보입니다"),

  // 사용자 관련 에러 (3000번대)
  USER_NOT_FOUND(3000, "사용자를 찾을 수 없습니다"),
  USER_ALREADY_EXISTS(3001, "이미 존재하는 사용자입니다"),
  INVALID_EMAIL_FORMAT(3002, "올바르지 않은 이메일 형식입니다"),
  INVALID_PASSWORD_FORMAT(3003, "올바르지 않은 비밀번호 형식입니다"),
  ACCOUNT_INACTIVE(3004, "비활성화된 계정입니다"),
  EMAIL_ALREADY_EXISTS(3005, "이미 사용 중인 이메일입니다"),

  // 데이터 관련 에러 (4000번대)
  DATA_NOT_FOUND(4000, "데이터를 찾을 수 없습니다"),
  DATA_ALREADY_EXISTS(4001, "이미 존재하는 데이터입니다"),
  DATA_INTEGRITY_VIOLATION(4002, "데이터 무결성 위반입니다"),

  // 파일 관련 에러 (5000번대)
  FILE_NOT_FOUND(5000, "파일을 찾을 수 없습니다"),
  FILE_TOO_LARGE(5001, "파일 크기가 너무 큽니다"),
  INVALID_FILE_TYPE(5002, "지원하지 않는 파일 형식입니다"),
  FILE_UPLOAD_FAILED(5003, "파일 업로드에 실패했습니다"),
  PDF_GENERATION_FAILED(5004, "PDF 생성에 실패했습니다"),
  FILE_SIZE_EXCEEDED(5005, "파일 크기 제한을 초과했습니다"),
  FILE_DOWNLOAD_FAILED(5006, "파일 다운로드에 실패했습니다"),
  EXTERNAL_API_ERROR(5007, "외부 API 호출 중 오류가 발생했습니다"),

  // 비즈니스 로직 에러 (6000번대)
  BUSINESS_RULE_VIOLATION(6000, "비즈니스 규칙 위반입니다"),
  INSUFFICIENT_PERMISSIONS(6001, "권한이 부족합니다"),
  OPERATION_NOT_ALLOWED(6002, "허용되지 않은 작업입니다"),

  // 이메일 검증 관련 에러 (7000번대)
  EMAIL_SEND_FAILED(7000, "이메일 발송에 실패했습니다"),
  VERIFICATION_CODE_NOT_FOUND(7001, "인증 코드를 찾을 수 없습니다"),
  VERIFICATION_CODE_INVALID(7002, "인증 코드가 올바르지 않습니다"),
  VERIFICATION_CODE_EXPIRED(7003, "인증 코드가 만료되었습니다"),
  VERIFICATION_ATTEMPTS_EXCEEDED(7004, "인증 시도 횟수를 초과했습니다"),
  EMAIL_ALREADY_VERIFIED(7005, "이미 인증된 이메일입니다"),
  VERIFICATION_CODE_ALREADY_SENT(7006, "인증 코드가 이미 발송되었습니다"),
  EMAIL_NOT_VERIFIED(7007, "이메일 인증이 완료되지 않았습니다"),

  // 요청 제한 관련 에러 (8000번대)
  TOO_MANY_REQUESTS(8000, "요청이 너무 많습니다"),
  TOO_MANY_ATTEMPTS(8001, "시도 횟수를 초과했습니다"),
  RESOURCE_NOT_FOUND(8002, "리소스를 찾을 수 없습니다"),

  // 이체 관련 에러 (9000번대)
  TRANSFER_RESULT_UNKNOWN(9000, "이체 결과를 확인 중입니다. 잠시 후 내역에서 확인해주세요"),
  TRANSFER_IN_PROGRESS(9001, "동일한 요청이 처리 중입니다"),
  TRANSFER_LIMIT_EXCEEDED(9002, "이체 한도를 초과했습니다"),
  TRANSFER_NOT_FOUND(9003, "이체 내역을 찾을 수 없습니다"),
  TRANSFER_NOT_CANCELLABLE(9004, "취소할 수 없는 상태의 이체입니다"),
  TRANSFER_APPROVAL_REQUIRED(9005, "보호자 승인이 필요한 이체입니다");

  private final int code;
  private final String message;
}
