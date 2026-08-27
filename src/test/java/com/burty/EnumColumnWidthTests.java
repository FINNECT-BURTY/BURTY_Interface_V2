package com.burty;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * {@code @Enumerated(STRING)} 컬럼의 선언 길이가 enum 상수를 담을 수 있는지 검증한다.
 *
 * <p>이 테스트가 필요한 이유: enum 에 긴 상수를 하나 추가하는 것만으로 조용히 데이터가 잘리거나 INSERT 가 실패할 수 있다. 실제로 {@code
 * Permission.VIEW_ALERT_AND_APPROVE}(22자)를 추가했을 때 기존 컬럼은 {@code varchar(20)} 이었다.
 *
 * <p>DB 없이 리플렉션만으로 검사하므로 빠르고, 마이그레이션을 잊은 경우를 커밋 시점에 잡는다. 길이를 명시하지 않은 컬럼은 Hibernate 가 상수 길이에 맞춰
 * 생성하므로 검사 대상에서 제외한다.
 */
class EnumColumnWidthTests {

  @Test
  @DisplayName("enum 상수가 선언된 컬럼 길이를 넘지 않는다")
  void enumConstantsFitDeclaredColumnLength() {
    List<String> violations = new ArrayList<>();

    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

    for (BeanDefinition definition : scanner.findCandidateComponents("com.burty.domain")) {
      Class<?> entity;
      try {
        entity = Class.forName(definition.getBeanClassName());
      } catch (ClassNotFoundException e) {
        continue;
      }
      for (Field field : entity.getDeclaredFields()) {
        Enumerated enumerated = field.getAnnotation(Enumerated.class);
        Column column = field.getAnnotation(Column.class);
        if (enumerated == null || column == null || enumerated.value() != EnumType.STRING) {
          continue;
        }
        if (!field.getType().isEnum()) {
          continue;
        }
        // length 를 명시하지 않으면 Hibernate 가 상수 길이에 맞춰 잡으므로 검사 불필요.
        // (기본값 255 는 "미지정" 과 구분되지 않지만, 255 미만으로 좁힌 경우만 위험하다.)
        int declared = column.length();
        if (declared >= 255) {
          continue;
        }
        for (Object constant : field.getType().getEnumConstants()) {
          String name = ((Enum<?>) constant).name();
          if (name.length() > declared) {
            violations.add(
                "%s.%s: 상수 '%s'(%d자) > 컬럼 길이 %d"
                    .formatted(
                        entity.getSimpleName(), field.getName(), name, name.length(), declared));
          }
        }
      }
    }
    assertTrue(
        violations.isEmpty(),
        "enum 상수가 컬럼 길이를 초과합니다. 마이그레이션으로 컬럼을 넓히세요:\n  " + String.join("\n  ", violations));
  }
}
