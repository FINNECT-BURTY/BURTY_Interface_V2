package com.burty.application.service.admin;

import com.burty.application.dto.admin.GrafanaDashboardSummaryResponse;
import com.burty.application.dto.admin.GrafanaEmbedResponse;
import com.burty.application.dto.admin.GrafanaPanelEmbedResponse;
import com.burty.config.GrafanaProperties;
import com.burty.core.error.enums.ErrorCode;
import com.burty.core.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GrafanaEmbedService {

  private final GrafanaProperties grafanaProperties;

  public GrafanaEmbedService(GrafanaProperties grafanaProperties) {
    this.grafanaProperties = grafanaProperties;
  }

  public List<GrafanaDashboardSummaryResponse> listDashboards() {
    ensureEnabled();
    return grafanaProperties.getDashboards().stream()
        .map(d -> new GrafanaDashboardSummaryResponse(d.getUid(), d.getSlug(), d.getTitle()))
        .toList();
  }

  public GrafanaEmbedResponse buildDashboardEmbed(String uid, String theme, Boolean kiosk) {
    ensureEnabled();
    GrafanaProperties.Dashboard dashboard = requireDashboard(uid);
    String resolvedTheme = resolveTheme(theme);
    boolean resolvedKiosk = kiosk != null ? kiosk : grafanaProperties.getEmbed().isKioskMode();

    String dashboardUrl = buildDashboardUrl(dashboard, resolvedTheme, resolvedKiosk, false);
    String iframeUrl = buildDashboardUrl(dashboard, resolvedTheme, resolvedKiosk, true);

    List<GrafanaPanelEmbedResponse> panels =
        dashboard.getPanels().stream()
            .map(
                panel ->
                    new GrafanaPanelEmbedResponse(
                        panel.getId(),
                        panel.getTitle(),
                        buildPanelUrl(dashboard, panel.getId(), resolvedTheme)))
            .toList();

    return new GrafanaEmbedResponse(
        dashboard.getUid(),
        dashboard.getTitle(),
        dashboardUrl,
        iframeUrl,
        resolvedTheme,
        resolvedKiosk,
        panels);
  }

  public GrafanaPanelEmbedResponse buildPanelEmbed(String uid, int panelId, String theme) {
    ensureEnabled();
    GrafanaProperties.Dashboard dashboard = requireDashboard(uid);
    GrafanaProperties.Panel panel =
        dashboard.getPanels().stream()
            .filter(p -> p.getId() == panelId)
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND, "패널을 찾을 수 없습니다: panelId=" + panelId));

    return new GrafanaPanelEmbedResponse(
        panel.getId(),
        panel.getTitle(),
        buildPanelUrl(dashboard, panel.getId(), resolveTheme(theme)));
  }

  private GrafanaProperties.Dashboard requireDashboard(String uid) {
    GrafanaProperties.Dashboard dashboard = grafanaProperties.findDashboard(uid);
    if (dashboard == null) {
      throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "대시보드를 찾을 수 없습니다: uid=" + uid);
    }
    return dashboard;
  }

  private void ensureEnabled() {
    if (!grafanaProperties.isEnabled()) {
      throw new BusinessException(
          ErrorCode.FORBIDDEN, "Grafana 임베딩이 비활성화되어 있습니다. burty.grafana.enabled=true 로 설정하세요.");
    }
  }

  private String resolveTheme(String theme) {
    if (theme == null || theme.isBlank()) {
      return grafanaProperties.getEmbed().getDefaultTheme();
    }
    return theme;
  }

  private String buildDashboardUrl(
      GrafanaProperties.Dashboard dashboard, String theme, boolean kiosk, boolean iframeFriendly) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(normalizeBase(grafanaProperties.getPublicBaseUrl()))
            .pathSegment("d", dashboard.getUid(), dashboard.getSlug())
            .queryParam("orgId", grafanaProperties.getDefaultOrgId())
            .queryParam("theme", theme)
            .queryParam("refresh", grafanaProperties.getEmbed().getRefreshSeconds() + "s");
    if (kiosk) {
      builder.queryParam("kiosk", iframeFriendly ? "tv" : "1");
    }
    return builder.build().toUriString();
  }

  private String buildPanelUrl(GrafanaProperties.Dashboard dashboard, int panelId, String theme) {
    return UriComponentsBuilder.fromUriString(normalizeBase(grafanaProperties.getPublicBaseUrl()))
        .pathSegment("d-solo", dashboard.getUid(), dashboard.getSlug())
        .queryParam("orgId", grafanaProperties.getDefaultOrgId())
        .queryParam("panelId", panelId)
        .queryParam("theme", theme)
        .queryParam("refresh", grafanaProperties.getEmbed().getRefreshSeconds() + "s")
        .build()
        .toUriString();
  }

  private static String normalizeBase(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }
}
