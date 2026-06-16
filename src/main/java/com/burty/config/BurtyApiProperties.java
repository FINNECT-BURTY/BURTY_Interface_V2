/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (BurtyApiProperties)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.config
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
package com.burty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "burty.api")
public class BurtyApiProperties {

  private boolean swaggerEnabled = true;
  private final RateLimit rateLimit = new RateLimit();

  public boolean isSwaggerEnabled() {
    return swaggerEnabled;
  }

  public void setSwaggerEnabled(boolean swaggerEnabled) {
    this.swaggerEnabled = swaggerEnabled;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public static class RateLimit {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
