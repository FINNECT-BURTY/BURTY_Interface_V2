/**
 *
 *
 * <pre>
 * <b>Description  : 관리 유스케이스 포트 (AiTemplateAdminUseCase)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.port.in.admin
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
package com.burty.application.port.in.admin;

import com.burty.application.dto.admin.AiFallbackTemplateRequest;
import com.burty.application.dto.admin.AiFallbackTemplateResponse;
import java.util.List;

public interface AiTemplateAdminUseCase {

  List<AiFallbackTemplateResponse> listTemplates();

  AiFallbackTemplateResponse upsert(AiFallbackTemplateRequest request);

  void deactivate(String templateKey);
}
