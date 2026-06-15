/**
 *
 *
 * <pre>
 * <b>Description  : 설정 설정 (ShedLockConfig)</b>
 * <b>Project Name : BURTY</b>
 * package  : com.burty.core.config
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
package com.burty.core.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    ensureShedLockTable(jdbcTemplate);
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(jdbcTemplate)
            .usingDbTime()
            .build());
  }

  private static void ensureShedLockTable(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS shedlock (
          name VARCHAR(64) NOT NULL PRIMARY KEY,
          lock_until TIMESTAMP(3) NOT NULL,
          locked_at TIMESTAMP(3) NOT NULL,
          locked_by VARCHAR(255) NOT NULL
        )
        """);
  }
}
