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

  /**
   * 최초 관리자 부트스트랩 허용 여부.
   *
   * <p>관리자 등록 엔드포인트는 이제 ROLE_ADMIN 을 요구한다. 그러면 첫 관리자를 만들 수 없으므로, 이 플래그가 켜져 있고 <b>아직 관리자가 한 명도 없을
   * 때만</b> 무인증 등록을 허용한다. 관리자가 하나라도 생기면 자동으로 닫힌다.
   *
   * <p>prod 에서는 {@code ProdStartupValidator} 가 강제로 false 를 요구한다.
   */
  private boolean bootstrapEnabled = false;

  public String getSetupKey() {
    return setupKey;
  }

  public void setSetupKey(String setupKey) {
    this.setupKey = setupKey;
  }

  public boolean isBootstrapEnabled() {
    return bootstrapEnabled;
  }

  public void setBootstrapEnabled(boolean bootstrapEnabled) {
    this.bootstrapEnabled = bootstrapEnabled;
  }
}
