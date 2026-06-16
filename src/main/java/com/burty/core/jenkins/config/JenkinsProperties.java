/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 프로퍼티 (JenkinsProperties)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.jenkins.config
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
package com.burty.core.jenkins.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Jenkins 연결 설정을 관리하는 Properties 클래스 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsProperties {

  private boolean enabled = false;

  // Jenkins 서버 URL
  private String url;

  // Jenkins 사용자명
  private String username;

  // Jenkins API 토큰
  private String token;

  // 연결 타임아웃 (밀리초)
  private Integer connectionTimeout = 30000;

  // 읽기 타임아웃 (밀리초)
  private Integer readTimeout = 60000;
}
