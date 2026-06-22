package com.burty.application.dto.admin;

import java.util.List;

public record GrafanaEmbedResponse(
    String dashboardUid,
    String title,
    String dashboardUrl,
    String iframeUrl,
    String theme,
    boolean kiosk,
    List<GrafanaPanelEmbedResponse> panels) {}
