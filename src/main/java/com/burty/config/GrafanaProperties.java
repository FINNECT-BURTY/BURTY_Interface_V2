package com.burty.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.grafana")
public class GrafanaProperties {

  private boolean enabled = false;
  private String baseUrl = "http://localhost:3001";
  private String publicBaseUrl = "http://localhost:3001";
  private int defaultOrgId = 1;
  private final Embed embed = new Embed();
  private final List<Dashboard> dashboards = new ArrayList<>();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl != null && !publicBaseUrl.isBlank() ? publicBaseUrl : baseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public int getDefaultOrgId() {
    return defaultOrgId;
  }

  public void setDefaultOrgId(int defaultOrgId) {
    this.defaultOrgId = defaultOrgId;
  }

  public Embed getEmbed() {
    return embed;
  }

  public List<Dashboard> getDashboards() {
    return dashboards;
  }

  public Dashboard findDashboard(String uid) {
    return dashboards.stream().filter(d -> uid.equals(d.getUid())).findFirst().orElse(null);
  }

  public static class Embed {
    private boolean kioskMode = true;
    private String defaultTheme = "light";
    private int refreshSeconds = 30;

    public boolean isKioskMode() {
      return kioskMode;
    }

    public void setKioskMode(boolean kioskMode) {
      this.kioskMode = kioskMode;
    }

    public String getDefaultTheme() {
      return defaultTheme;
    }

    public void setDefaultTheme(String defaultTheme) {
      this.defaultTheme = defaultTheme;
    }

    public int getRefreshSeconds() {
      return refreshSeconds;
    }

    public void setRefreshSeconds(int refreshSeconds) {
      this.refreshSeconds = refreshSeconds;
    }
  }

  public static class Dashboard {
    private String uid = "burty-overview";
    private String slug = "burty-overview";
    private String title = "BURTY 운영 대시보드";
    private final List<Panel> panels = new ArrayList<>();

    public String getUid() {
      return uid;
    }

    public void setUid(String uid) {
      this.uid = uid;
    }

    public String getSlug() {
      return slug;
    }

    public void setSlug(String slug) {
      this.slug = slug;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public List<Panel> getPanels() {
      return panels;
    }
  }

  public static class Panel {
    private int id;
    private String title;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }
  }
}
