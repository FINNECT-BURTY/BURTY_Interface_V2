/**
 *
 *
 * <pre>
 * <b>Description  : 코어 API 컨트롤러 (BaseController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.controller
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
package com.burty.core.controller;

import com.burty.core.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// 공통 API base path(/api/v1) 는 BurtyWebConfig.configurePathMatch() 에서
// com.burty.adapter.in.web 패키지의 @RestController 에 자동으로 prepend 된다.
@Slf4j
public abstract class BaseController {

  protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
    return ResponseEntity.ok(ApiResponse.ok(data));
  }

  protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
    return ResponseEntity.ok(ApiResponse.success(data, message));
  }

  protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
    return ResponseEntity.status(201).body(ApiResponse.success(data, message));
  }

  protected <T> ResponseEntity<ApiResponse<Page<T>>> okPage(Page<T> page) {
    return ResponseEntity.ok(ApiResponse.ok(page));
  }

  protected String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      return authentication.getName();
    }
    return null;
  }

  protected Authentication getCurrentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
