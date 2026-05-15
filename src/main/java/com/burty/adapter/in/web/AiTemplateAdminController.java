package com.burty.adapter.in.web;

import com.burty.adapter.in.web.dto.AiFallbackTemplateRequest;
import com.burty.adapter.in.web.dto.SimpleResultResponse;
import com.burty.core.dto.response.ApiResponse;
import com.burty.domain.entity.AiFallbackTemplateEntity;
import com.burty.domain.repository.AiFallbackTemplateRepository;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/burty/admin/ai-templates")
@Tag(name = "BURTY AI Template Admin", description = "AI fallback 문구 템플릿 관리 API")
public class AiTemplateAdminController {
    private final AiFallbackTemplateRepository repository;

    public AiTemplateAdminController(AiFallbackTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<List<AiFallbackTemplateEntity>> templates() {
        return ApiResponse.ok(repository.findAll());
    }

    @PostMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<AiFallbackTemplateEntity> upsert(@RequestBody AiFallbackTemplateRequest request) {
        AiFallbackTemplateEntity entity = repository.findById(request.getTemplateKey()).orElseGet(AiFallbackTemplateEntity::new);
        entity.setTemplateKey(request.getTemplateKey());
        entity.setRiskLevel(request.getRiskLevel());
        entity.setOccupationCode(request.getOccupationCode());
        entity.setCauseType(request.getCauseType());
        entity.setTemplateText(request.getTemplateText());
        entity.setActive(request.getActive() == null || request.getActive());
        return ApiResponse.ok(repository.save(entity));
    }

    @DeleteMapping("/{templateKey}")
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<SimpleResultResponse> deactivate(@PathVariable String templateKey) {
        AiFallbackTemplateEntity entity = repository.findById(templateKey).orElseThrow();
        entity.setActive(false);
        repository.save(entity);
        return ApiResponse.ok(new SimpleResultResponse(true, "AI fallback 템플릿이 비활성화되었습니다."));
    }
}
