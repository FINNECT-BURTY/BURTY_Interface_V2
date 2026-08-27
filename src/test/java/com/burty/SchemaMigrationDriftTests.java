package com.burty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.support.IntegrationTestBase;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Flyway 마이그레이션으로 만든 스키마가 JPA 엔티티 모델과 일치하는지 검증한다.
 *
 * <p>컨텍스트 로딩 자체가 {@code ddl-auto=validate} 를 통과해야 하므로, 이 테스트가 뜨는 것만으로 이미 드리프트가 없다는 뜻이다. 아래 단언들은
 * 마이그레이션이 실제로 적용됐는지(= validate 가 빈 스키마를 통과한 게 아닌지) 를 확인한다.
 *
 * <p>Docker 가 없는 환경에서는 H2 + create-drop 으로 강등되므로 검증 의미가 없어 스킵한다.
 */
@SpringBootTest
@EnabledIf("com.burty.support.MariaDbTestContainer#isRealDatabase")
class SchemaMigrationDriftTests extends IntegrationTestBase {

  @Autowired private DataSource dataSource;

  @Test
  void flywayMigrationsAreApplied() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Integer applied =
        jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);
    assertTrue(applied != null && applied > 0, "Flyway 마이그레이션이 적용되지 않았습니다");
  }

  @Test
  void baselineCreatedCoreTables() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    for (String table :
        new String[] {
          "tbl_user", "tbl_transfer_order", "tbl_transfer_record", "tbl_outbox_event"
        }) {
      Integer count =
          jdbc.queryForObject(
              "select count(*) from information_schema.tables"
                  + " where table_schema = database() and table_name = ?",
              Integer.class,
              table);
      assertTrue(count != null && count == 1, table + " 테이블이 없습니다");
    }
  }

  @Test
  void transferOrderHasPerUserIdempotencyUniqueConstraint() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    // (user_id, idempotency_key) 복합 유니크가 있어야 동시 요청 중복 이체를 DB 가 막는다.
    Integer columns =
        jdbc.queryForObject(
            "select count(*) from information_schema.statistics s"
                + " where s.table_schema = database() and s.table_name = 'tbl_transfer_order'"
                + " and s.non_unique = 0 and s.column_name in ('user_id','idempotency_key')"
                + " and s.index_name = ("
                + "   select index_name from information_schema.statistics"
                + "   where table_schema = database() and table_name = 'tbl_transfer_order'"
                + "   and non_unique = 0 and column_name = 'idempotency_key' limit 1)",
            Integer.class);
    assertTrue(
        columns != null && columns == 2,
        "tbl_transfer_order 에 (user_id, idempotency_key) 유니크 제약이 없습니다");
  }

  @Test
  void legacyGlobalIdempotencyConstraintIsGone() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Integer legacy =
        jdbc.queryForObject(
            "select count(*) from information_schema.statistics"
                + " where table_schema = database() and table_name = 'tbl_transfer_order'"
                + " and index_name = 'uk_idempotency'",
            Integer.class);
    assertFalse(
        legacy != null && legacy > 0, "전역 유니크 uk_idempotency 가 남아 있습니다 (사용자 간 키 충돌로 500 발생)");
  }
}
