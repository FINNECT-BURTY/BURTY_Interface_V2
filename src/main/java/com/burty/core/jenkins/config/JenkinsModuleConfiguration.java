/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (JenkinsModuleConfiguration)</b>
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "jenkins", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JenkinsProperties.class)
@ComponentScan(basePackages = "com.burty.core.jenkins")
public class JenkinsModuleConfiguration {}
