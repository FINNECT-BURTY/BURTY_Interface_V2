package com.burty.adapter.in.web;

import com.burty.application.port.in.BaseCodeUseCase;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.BaseCodeEntity;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/burty/codes")
@Tag(name = "BURTY Base Code", description = "기준정보(tbl_code) 조회/관리 API")
public class BaseCodeController {

    private final BaseCodeUseCase baseCodeUseCase;

    public BaseCodeController(BaseCodeUseCase baseCodeUseCase) {
        this.baseCodeUseCase = baseCodeUseCase;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "코드 그룹 조회", description = "사용 중(use_yn=Y)인 코드를 sort_order 순으로 반환합니다.")
    public ApiResponse<List<CodeItem>> listByGroup(@RequestParam("group") String group) {
        List<CodeItem> items = baseCodeUseCase.lookup(group).stream()
                .map(CodeItem::from)
                .toList();
        return ApiResponse.ok(items);
    }

    @GetMapping("/{group}/{value}")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "단일 코드 조회")
    public ApiResponse<Map<String, Object>> get(@PathVariable String group, @PathVariable String value) {
        return baseCodeUseCase.lookup(group, value)
                .map(e -> ApiResponse.ok(Map.<String, Object>of(
                        "found", true,
                        "code", CodeItem.from(e)
                )))
                .orElseGet(() -> ApiResponse.ok(Map.of("found", false)));
    }

    @GetMapping("/children/{parentCodeId}")
    @AuthLevel(RiskLevel.LEVEL_1)
    @Operation(summary = "하위 코드 목록")
    public ApiResponse<List<CodeItem>> children(@PathVariable String parentCodeId) {
        return ApiResponse.ok(baseCodeUseCase.children(parentCodeId).stream().map(CodeItem::from).toList());
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "코드 등록/수정", description = "관리자 전용. LEVEL_3 인증 필요.")
    public ApiResponse<CodeItem> upsert(@RequestBody UpsertRequest request) {
        BaseCodeEntity entity = request.toEntity();
        BaseCodeEntity saved = baseCodeUseCase.upsert(entity, currentUser());
        return ApiResponse.ok(CodeItem.from(saved));
    }

    @DeleteMapping("/{codeId}")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "코드 비활성화", description = "물리 삭제 대신 use_yn=N으로 표시.")
    public ApiResponse<Map<String, Object>> deactivate(@PathVariable String codeId) {
        baseCodeUseCase.deactivate(codeId, currentUser());
        return ApiResponse.ok(Map.of("deactivated", true, "codeId", codeId));
    }

    @PostMapping("/reload")
    @AuthLevel(RiskLevel.LEVEL_3)
    @Operation(summary = "캐시 재적재")
    public ApiResponse<Map<String, Object>> reload() {
        baseCodeUseCase.reload();
        return ApiResponse.ok(Map.of("reloaded", true));
    }

    private String currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
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
            String attr5
    ) {
        public static CodeItem from(BaseCodeEntity e) {
            return new CodeItem(
                    e.getCodeId(), e.getCodeGroup(), e.getCodeValue(),
                    e.getCodeNameKo(), e.getCodeNameEn(),
                    e.getParentCodeId(), e.getSortOrder(), e.getUseYn(), e.getDescription(),
                    e.getAttr1(), e.getAttr2(), e.getAttr3(), e.getAttr4(), e.getAttr5()
            );
        }
    }

    public static class UpsertRequest {
        private String codeId;
        private String codeGroup;
        private String codeValue;
        private String codeNameKo;
        private String codeNameEn;
        private String parentCodeId;
        private Integer sortOrder;
        private String useYn;
        private String description;
        private String attr1;
        private String attr2;
        private String attr3;
        private String attr4;
        private String attr5;

        public BaseCodeEntity toEntity() {
            BaseCodeEntity e = new BaseCodeEntity();
            e.setCodeId(codeId);
            e.setCodeGroup(codeGroup);
            e.setCodeValue(codeValue);
            e.setCodeNameKo(codeNameKo);
            e.setCodeNameEn(codeNameEn);
            e.setParentCodeId(parentCodeId);
            e.setSortOrder(sortOrder);
            e.setUseYn(useYn);
            e.setDescription(description);
            e.setAttr1(attr1);
            e.setAttr2(attr2);
            e.setAttr3(attr3);
            e.setAttr4(attr4);
            e.setAttr5(attr5);
            return e;
        }

        public String getCodeId() { return codeId; }
        public void setCodeId(String codeId) { this.codeId = codeId; }
        public String getCodeGroup() { return codeGroup; }
        public void setCodeGroup(String codeGroup) { this.codeGroup = codeGroup; }
        public String getCodeValue() { return codeValue; }
        public void setCodeValue(String codeValue) { this.codeValue = codeValue; }
        public String getCodeNameKo() { return codeNameKo; }
        public void setCodeNameKo(String codeNameKo) { this.codeNameKo = codeNameKo; }
        public String getCodeNameEn() { return codeNameEn; }
        public void setCodeNameEn(String codeNameEn) { this.codeNameEn = codeNameEn; }
        public String getParentCodeId() { return parentCodeId; }
        public void setParentCodeId(String parentCodeId) { this.parentCodeId = parentCodeId; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public String getUseYn() { return useYn; }
        public void setUseYn(String useYn) { this.useYn = useYn; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAttr1() { return attr1; }
        public void setAttr1(String attr1) { this.attr1 = attr1; }
        public String getAttr2() { return attr2; }
        public void setAttr2(String attr2) { this.attr2 = attr2; }
        public String getAttr3() { return attr3; }
        public void setAttr3(String attr3) { this.attr3 = attr3; }
        public String getAttr4() { return attr4; }
        public void setAttr4(String attr4) { this.attr4 = attr4; }
        public String getAttr5() { return attr5; }
        public void setAttr5(String attr5) { this.attr5 = attr5; }
    }
}