package com.berty.core.handler;

import com.berty.core.dto.response.ApiResponse;
import com.berty.core.error.dto.ErrorResponse;
import com.berty.core.error.enums.ErrorCode;
import com.berty.core.exception.BusinessException;
import com.berty.core.exception.ResourceNotFoundException;
import com.berty.core.jenkins.exception.JenkinsBuildException;
import com.berty.core.jenkins.exception.JenkinsConnectionException;
import com.berty.core.jenkins.exception.JenkinsException;
import com.berty.core.jenkins.exception.JenkinsJobNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BusinessException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                e.getErrorCode(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 커스텀 인증 예외 처리
     */
    @ExceptionHandler(com.berty.core.exception.AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthenticationException(com.berty.core.exception.AuthenticationException e,
                                                                                    HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                e.getErrorCode(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 리소스 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.ENTITY_NOT_FOUND,
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Spring Security 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSpringAuthenticationException(AuthenticationException e,
                                                                                          HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_CREDENTIALS,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Spring Security 권한 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Spring Security 메서드 권한 예외 처리
     */
    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAuthorizationDeniedException(org.springframework.security.authorization.AuthorizationDeniedException e,
                                                                                         HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 잘못된 인증 정보 예외 처리
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_CREDENTIALS,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 유효성 검증 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(),
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 바인딩 예외 처리
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBindException(BindException e, HttpServletRequest request) {

        List<String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        String errorMessage = String.join(", ", errors);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(),
                errorMessage
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 메서드 인자 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(),
                "Parameter '" + e.getName() + "' should be of type " + e.getRequiredType().getSimpleName()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 지원하지 않는 HTTP 메서드 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.METHOD_NOT_ALLOWED,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 핸들러 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.ENTITY_NOT_FOUND,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Jenkins 연결 예외 처리
     */
    @ExceptionHandler(JenkinsConnectionException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJenkinsConnectionException(JenkinsConnectionException e, HttpServletRequest request) {
        log.error("Jenkins connection error", e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                "Jenkins server connection failed: " + e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Jenkins Job 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(JenkinsJobNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJenkinsJobNotFoundException(JenkinsJobNotFoundException e, HttpServletRequest request) {
        log.error("Jenkins job not found", e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.ENTITY_NOT_FOUND,
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Jenkins 빌드 예외 처리
     */
    @ExceptionHandler(JenkinsBuildException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJenkinsBuildException(JenkinsBuildException e, HttpServletRequest request) {
        log.error("Jenkins build error", e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                "Jenkins build failed: " + e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * Jenkins 일반 예외 처리
     */
    @ExceptionHandler(JenkinsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJenkinsException(JenkinsException e, HttpServletRequest request) {
        log.error("Jenkins error", e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                "Jenkins error: " + e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("NullPointerException: URI={}", request.getRequestURI(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * IndexOutOfBoundsException 처리
     */
    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIndexOutOfBoundsException(IndexOutOfBoundsException e, HttpServletRequest request) {
        log.error("IndexOutOfBoundsException: URI={}", request.getRequestURI(), e);

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.error("IllegalArgumentException: URI={}, message={}", request.getRequestURI(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e, HttpServletRequest request) throws Exception {
        // 클라이언트가 연결을 끊은 경우 (동영상 스트리밍 range request 중 정상 종료) - Spring에 위임
        if (e instanceof org.apache.catalina.connector.ClientAbortException
                || e.getCause() instanceof org.apache.catalina.connector.ClientAbortException) {
            throw e;
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse.getMessage(), String.valueOf(errorResponse.getCode())));
    }
}
