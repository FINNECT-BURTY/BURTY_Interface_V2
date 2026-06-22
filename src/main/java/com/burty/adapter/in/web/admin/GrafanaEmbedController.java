package com.burty.adapter.in.web.admin;

import com.burty.application.dto.admin.GrafanaDashboardSummaryResponse;
import com.burty.application.dto.admin.GrafanaEmbedResponse;
import com.burty.application.dto.admin.GrafanaPanelEmbedResponse;
import com.burty.application.service.admin.GrafanaEmbedService;
import com.burty.core.controller.BaseController;
import com.burty.core.dto.response.ApiResponse;
import com.burty.security.AuthLevel;
import com.burty.security.RiskLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/observability")
@RequiredArgsConstructor
@Tag(name = "BURTY Observability", description = "Grafana 대시보드 임베딩 API (ADMIN)")
public class GrafanaEmbedController extends BaseController {

  private final GrafanaEmbedService grafanaEmbedService;

  @GetMapping("/dashboards")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "임베딩 가능한 Grafana 대시보드 목록")
  public ApiResponse<List<GrafanaDashboardSummaryResponse>> dashboards() {
    return ApiResponse.ok(grafanaEmbedService.listDashboards());
  }

  @GetMapping("/embed/dashboard")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "대시보드 iframe 임베드 URL 생성")
  public ApiResponse<GrafanaEmbedResponse> embedDashboard(
      @RequestParam(defaultValue = "burty-overview") String uid,
      @RequestParam(required = false) String theme,
      @RequestParam(required = false) Boolean kiosk) {
    return ApiResponse.ok(grafanaEmbedService.buildDashboardEmbed(uid, theme, kiosk));
  }

  @GetMapping("/embed/panel")
  @AuthLevel(RiskLevel.LEVEL_2)
  @Operation(summary = "단일 패널 iframe 임베드 URL 생성")
  public ApiResponse<GrafanaPanelEmbedResponse> embedPanel(
      @RequestParam(defaultValue = "burty-overview") String uid,
      @RequestParam int panelId,
      @RequestParam(required = false) String theme) {
    return ApiResponse.ok(grafanaEmbedService.buildPanelEmbed(uid, panelId, theme));
  }
}
