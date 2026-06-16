/**
 *
 *
 * <pre>
 * <b>Description  : 관리 애플리케이션 서비스 (AuditLogService)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.application.service.admin
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
package com.burty.application.service.admin;

import com.burty.adapter.in.web.mapper.WebResponseMapper;
import com.burty.application.dto.admin.AuditLogResponse;
import com.burty.application.port.in.admin.AuditLogUseCase;
import com.burty.domain.admin.repository.AuditLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService implements AuditLogUseCase {

  private final AuditLogRepository auditLogRepository;
  private final WebResponseMapper webResponseMapper;

  @Override
  @Transactional(readOnly = true)
  public List<AuditLogResponse> listRecent(int size) {
    int capped = Math.min(200, Math.max(1, size));
    return auditLogRepository
        .findAll(PageRequest.of(0, capped, Sort.by(Sort.Direction.DESC, "occurredAt")))
        .stream()
        .map(webResponseMapper::toResponse)
        .toList();
  }
}
