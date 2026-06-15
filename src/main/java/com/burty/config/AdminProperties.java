/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (AdminProperties)</b>
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
@ConfigurationProperties(prefix = "burty.admin")
public class AdminProperties {

  private String setupKey = "";

  public String getSetupKey() {
    return setupKey;
  }

  public void setSetupKey(String setupKey) {
    this.setupKey = setupKey;
  }
}
