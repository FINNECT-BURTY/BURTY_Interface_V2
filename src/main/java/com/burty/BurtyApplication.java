/**
 *
 *
 * <pre>
 * <b>Description  : BURTY Spring Boot 애플리케이션 진입점</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty
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
package com.burty;

import com.burty.core.jenkins.config.JenkinsModuleConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = RetryAutoConfiguration.class)
@Import(JenkinsModuleConfiguration.class)
@ComponentScan(
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.burty\\.core\\.jenkins\\..*"))
public class BurtyApplication {

  public static void main(String[] args) {
    SpringApplication.run(BurtyApplication.class, args);
  }
}
