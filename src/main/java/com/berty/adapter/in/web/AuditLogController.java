package com.berty.adapter.in.web;

import com.berty.adapter.in.web.dto.AuditLogResponse;
import com.berty.core.dto.response.ApiResponse;
import com.berty.domain.entity.AuditLogEntity;
import com.berty.domain.repository.AuditLogRepository;
import com.berty.security.AuthLevel;
import com.berty.security.RiskLevel;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/berty/audit-logs")
@Tag(name = "BERTY Audit Logs", description = "로그인/생체/마이데이터/정책/추천 실행 감사로그 조회 API")
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @AuthLevel(RiskLevel.LEVEL_3)
    public ApiResponse<List<AuditLogResponse>> logs(@RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(auditLogRepository.findAll(PageRequest.of(0, Math.min(200, size), Sort.by(Sort.Direction.DESC, "occurredAt")))
                .stream()
                .map(this::toResponse)
                .toList());
    }

    private AuditLogResponse toResponse(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getAuditId(),
                entity.getOccurredAt(),
                entity.getActorType() == null ? null : entity.getActorType().name(),
                entity.getAction(),
                entity.getResult() == null ? null : entity.getResult().name(),
                entity.getTargetType(),
                entity.getMetadata()
        );
    }
}
