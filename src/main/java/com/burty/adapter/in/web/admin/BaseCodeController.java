/**
 *
 *
 * <pre>
 * <b>Description  : 관리 API 컨트롤러 (BaseCodeController)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.adapter.in.web.admin
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
package com.burty.adapter.in.web.admin;

import com.burty.application.dto.admin.BaseCodeDeactivateResponse;
import com.burty.application.dto.admin.BaseCodeLookupResponse;
import com.burty.application.dto.admin.BaseCodeReloadResponse;
import com.burty.application.dto.admin.BaseCodeUpsertRequest;
import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.admin.entity.BaseCodeEntity;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/codes")
@Tag(name = "BURTY Base Code", description = "기준정보(tbl_code) 조회/관리 API")
@RequiredArgsConstructor
public class BaseCodeController extends BaseController {

  private final BaseCodeUseCase baseCodeUseCase;

  @GetMapping
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "코드 그룹 조회", description = "사용 중(use_yn=Y)인 코드를 sort_order 순으로 반환합니다.")
  public ApiResponse<List<CodeItem>> listByGroup(@RequestParam("group") String group) {
    List<CodeItem> items = baseCodeUseCase.lookup(group).stream().map(CodeItem::from).toList();
    return ApiResponse.ok(items);
  }

  @GetMapping("/{group}/{value}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "단일 코드 조회")
  public ApiResponse<BaseCodeLookupResponse> get(
      @PathVariable String group, @PathVariable String value) {
    return baseCodeUseCase
        .lookup(group, value)
        .map(e -> ApiResponse.ok(BaseCodeLookupResponse.found(CodeItem.from(e))))
        .orElseGet(() -> ApiResponse.ok(BaseCodeLookupResponse.notFound()));
  }

  @GetMapping("/children/{parentCodeId}")
  @AuthLevel(RiskLevel.LEVEL_1)
  @Operation(summary = "하위 코드 목록")
  public ApiResponse<List<CodeItem>> children(@PathVariable String parentCodeId) {
    return ApiResponse.ok(
        baseCodeUseCase.children(parentCodeId).stream().map(CodeItem::from).toList());
  }

  @PostMapping
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "코드 등록/수정", description = "관리자 전용. LEVEL_3 인증 필요.")
  public ApiResponse<CodeItem> upsert(@Valid @RequestBody BaseCodeUpsertRequest request) {
    BaseCodeEntity entity = request.toEntity();
    BaseCodeEntity saved = baseCodeUseCase.upsert(entity, currentUser());
    return ApiResponse.ok(CodeItem.from(saved));
  }

  @DeleteMapping("/{codeId}")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "코드 비활성화", description = "물리 삭제 대신 use_yn=N으로 표시.")
  public ApiResponse<BaseCodeDeactivateResponse> deactivate(@PathVariable String codeId) {
    baseCodeUseCase.deactivate(codeId, currentUser());
    return ApiResponse.ok(new BaseCodeDeactivateResponse(true, codeId));
  }

  @PostMapping("/reload")
  @AuthLevel(RiskLevel.LEVEL_3)
  @Operation(summary = "캐시 재적재")
  public ApiResponse<BaseCodeReloadResponse> reload() {
    baseCodeUseCase.reload();
    return ApiResponse.ok(new BaseCodeReloadResponse(true));
  }

  private String currentUser() {
    Object principal =
        SecurityContextHolder.getContext().getAuthentication() == null
            ? "system"
            : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return String.valueOf(principal);
  }

  public record CodeItem(
      String codeId,
      String codeGroup,
      String codeValue,
      String codeNameKo,
      String codeNameEn,
      String parentCodeId,
      Integer sortOrder,
      String useYn,
      String description,
      String attr1,
      String attr2,
      String attr3,
      String attr4,
      String attr5) {
    public static CodeItem from(BaseCodeEntity e) {
      return new CodeItem(
          e.getCodeId(),
          e.getCodeGroup(),
          e.getCodeValue(),
          e.getCodeNameKo(),
          e.getCodeNameEn(),
          e.getParentCodeId(),
          e.getSortOrder(),
          e.getUseYn(),
          e.getDescription(),
          e.getAttr1(),
          e.getAttr2(),
          e.getAttr3(),
          e.getAttr4(),
          e.getAttr5());
    }
  }
}
