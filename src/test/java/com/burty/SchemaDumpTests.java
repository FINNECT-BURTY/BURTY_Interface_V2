package com.burty;

import com.burty.support.IntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Tag("schema-dump")
@TestPropertySource(
    properties = {
      "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
      "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=build/generated-schema.sql",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect",
      "spring.jpa.properties.hibernate.type.preferred_enum_jdbc_type=VARCHAR",
      "spring.jpa.properties.hibernate.hbm2ddl.delimiter=;"
    })
/**
 * Flyway 베이스라인 재생성용 유틸리티. 평소에는 실행하지 않는다.
 *
 * <p>엔티티를 바꾼 뒤 베이스라인을 다시 만들려면:
 *
 * <pre>
 *   ./gradlew test --tests com.burty.SchemaDumpTests -Dtest.schemaDump=true
 *   python3 tools/generate_baseline.py
 * </pre>
 *
 * <p>주의: V3 는 이미 적용된 환경이 있을 수 있다. 재생성 결과를 V3 에 덮어쓰면 Flyway 체크섬이 깨진다. 스키마 변경은 새 V 마이그레이션으로 추가하고,
 * 재생성은 V3 가 아직 어디에도 적용되지 않았을 때만 하라.
 */
class SchemaDumpTests extends IntegrationTestBase {

  @Test
  void dump() {}
}
