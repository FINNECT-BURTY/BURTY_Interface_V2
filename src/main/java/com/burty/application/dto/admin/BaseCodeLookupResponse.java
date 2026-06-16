/**
 *
 *
 * <pre>
 * <b>Description  : 관리 응답 DTO (BaseCodeLookupResponse)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.dto.admin
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
package com.burty.application.dto.admin;

import com.burty.adapter.in.web.admin.BaseCodeController;

public record BaseCodeLookupResponse(boolean found, BaseCodeController.CodeItem code) {
  public static BaseCodeLookupResponse notFound() {
    return new BaseCodeLookupResponse(false, null);
  }

  public static BaseCodeLookupResponse found(BaseCodeController.CodeItem code) {
    return new BaseCodeLookupResponse(true, code);
  }
}
