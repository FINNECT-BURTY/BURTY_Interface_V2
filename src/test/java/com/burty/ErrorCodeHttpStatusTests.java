/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (ErrorCodeHttpStatusTests)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
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
package com.burty;

import com.burty.core.error.enums.ErrorCode;
import com.burty.core.error.enums.ErrorCodeHttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeHttpStatusTests {

  @Test
  void unauthorizedMapsTo401() {
    Assertions.assertEquals(
        HttpStatus.UNAUTHORIZED, ErrorCodeHttpStatus.resolve(ErrorCode.UNAUTHORIZED));
  }

  @Test
  void forbiddenMapsTo403() {
    Assertions.assertEquals(HttpStatus.FORBIDDEN, ErrorCodeHttpStatus.resolve(ErrorCode.FORBIDDEN));
  }

  @Test
  void tooManyRequestsMapsTo429() {
    Assertions.assertEquals(
        HttpStatus.TOO_MANY_REQUESTS, ErrorCodeHttpStatus.resolve(ErrorCode.TOO_MANY_REQUESTS));
  }

  @Test
  void invalidInputMapsTo400() {
    Assertions.assertEquals(
        HttpStatus.BAD_REQUEST, ErrorCodeHttpStatus.resolve(ErrorCode.INVALID_INPUT_VALUE));
  }
}
