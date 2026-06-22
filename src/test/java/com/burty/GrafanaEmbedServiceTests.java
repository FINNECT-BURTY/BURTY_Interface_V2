package com.burty;

import com.burty.application.dto.admin.GrafanaEmbedResponse;
import com.burty.application.service.admin.GrafanaEmbedService;
import com.burty.config.GrafanaProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GrafanaEmbedServiceTests {

  @Test
  void buildDashboardEmbed_returnsIframeUrls() {
    GrafanaProperties properties = new GrafanaProperties();
    properties.setEnabled(true);
    properties.setPublicBaseUrl("http://localhost:3001");

    GrafanaProperties.Dashboard dashboard = new GrafanaProperties.Dashboard();
    dashboard.setUid("burty-overview");
    dashboard.setSlug("burty-overview");
    dashboard.setTitle("BURTY 운영 대시보드");

    GrafanaProperties.Panel panel = new GrafanaProperties.Panel();
    panel.setId(1);
    panel.setTitle("HTTP Request Rate");
    dashboard.getPanels().add(panel);
    properties.getDashboards().add(dashboard);

    GrafanaEmbedService service = new GrafanaEmbedService(properties);
    GrafanaEmbedResponse response = service.buildDashboardEmbed("burty-overview", "dark", true);

    Assertions.assertEquals("burty-overview", response.dashboardUid());
    Assertions.assertTrue(response.iframeUrl().contains("/d/burty-overview/burty-overview"));
    Assertions.assertTrue(response.iframeUrl().contains("kiosk=tv"));
    Assertions.assertTrue(response.iframeUrl().contains("theme=dark"));
    Assertions.assertEquals(1, response.panels().size());
    Assertions.assertTrue(response.panels().get(0).iframeUrl().contains("/d-solo/"));
    Assertions.assertTrue(response.panels().get(0).iframeUrl().contains("panelId=1"));
  }

  @Test
  void buildDashboardEmbed_whenDisabled_throws() {
    GrafanaProperties properties = new GrafanaProperties();
    properties.setEnabled(false);
    GrafanaEmbedService service = new GrafanaEmbedService(properties);
    Assertions.assertThrows(
        Exception.class, () -> service.buildDashboardEmbed("burty-overview", null, null));
  }
}
