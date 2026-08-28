package com.burty;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.burty.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@code ObjectMapper} 빈의 직렬화 계약.
 *
 * <p>이 빈은 API 응답과 아웃박스 페이로드, Redis 캐시 값을 모두 직렬화한다. 설정이 바뀌면 클라이언트가 받는 JSON 이 조용히 바뀌고, 이미 저장된 아웃박스
 * 페이로드를 읽지 못하게 된다.
 *
 * <p>특히 날짜다. 모듈 등록이 빠지면 {@code LocalDate} 가 <b>배열</b>({@code [2026,8,28]})이나 에포크 숫자로 나가는데, 컴파일도
 * 통과하고 예외도 나지 않는다. 클라이언트에서 깨져야 안다.
 *
 * <p>그래서 빌더를 바꾸든 Jackson 버전을 올리든, 이 테스트가 통과해야 한다.
 */
@SpringBootTest
class ObjectMapperContractTests extends IntegrationTestBase {

  @Autowired private ObjectMapper objectMapper;

  record Sample(String name, LocalDate date, LocalDateTime timestamp) {}

  @Test
  @DisplayName("LocalDate 는 ISO 문자열로 직렬화된다 (배열이나 숫자가 아니다)")
  void datesSerializeAsIsoStrings() throws Exception {
    String json =
        objectMapper.writeValueAsString(
            new Sample("a", LocalDate.of(2026, 8, 28), LocalDateTime.of(2026, 8, 28, 9, 30, 0)));

    assertTrue(json.contains("\"date\":\"2026-08-28\""), "LocalDate 가 ISO 문자열이 아니다: " + json);
    assertTrue(
        json.contains("\"timestamp\":\"2026-08-28T09:30:00\""),
        "LocalDateTime 이 ISO 문자열이 아니다: " + json);
  }

  @Test
  @DisplayName("ISO 문자열을 날짜로 역직렬화한다")
  void datesDeserializeFromIsoStrings() throws Exception {
    Sample parsed =
        objectMapper.readValue(
            "{\"name\":\"a\",\"date\":\"2026-08-28\",\"timestamp\":\"2026-08-28T09:30:00\"}",
            Sample.class);

    assertEquals(LocalDate.of(2026, 8, 28), parsed.date());
    assertEquals(LocalDateTime.of(2026, 8, 28, 9, 30, 0), parsed.timestamp());
  }

  @Test
  @DisplayName("예전에 배열로 저장된 날짜도 그대로 읽힌다")
  void legacyArrayDatesStillDeserialize() throws Exception {
    // 예전 설정은 LocalDate 를 [2026,8,28] 배열로 썼다. 감사 로그와 아웃박스 페이로드에
    // 그 형식으로 저장된 데이터가 남아 있으므로, 형식을 바꾼 뒤에도 읽을 수 있어야 한다.
    Sample parsed =
        objectMapper.readValue(
            "{\"name\":\"a\",\"date\":[2026,8,28],\"timestamp\":[2026,8,28,9,30]}", Sample.class);

    assertEquals(LocalDate.of(2026, 8, 28), parsed.date());
    assertEquals(LocalDateTime.of(2026, 8, 28, 9, 30, 0), parsed.timestamp());
  }

  @Test
  @DisplayName("모르는 필드가 있어도 역직렬화가 깨지지 않는다")
  void unknownPropertiesAreIgnored() {
    // 외부 연동 응답과 예전 버전의 아웃박스 페이로드에는 우리가 모르는 필드가 섞인다.
    // 여기서 예외가 나면 이벤트가 DEAD 로 격리된다.
    assertDoesNotThrow(
        () ->
            objectMapper.readValue(
                "{\"name\":\"a\",\"date\":\"2026-08-28\",\"timestamp\":null,\"unknown\":1}",
                Sample.class));
    assertFalse(
        objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
        "FAIL_ON_UNKNOWN_PROPERTIES 가 켜졌다");
  }

  @Test
  @DisplayName("기본 뷰 포함이 꺼져 있다")
  void defaultViewInclusionIsDisabled() {
    // 켜져 있으면 @JsonView 를 쓰는 순간 뷰에 없는 필드까지 전부 나간다.
    assertFalse(objectMapper.isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
  }
}
