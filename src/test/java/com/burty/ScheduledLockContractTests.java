package com.burty;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 스케줄이 걸린 메서드에는 분산 락이 함께 있어야 한다.
 *
 * <p>인스턴스가 여러 대면 {@code @Scheduled} 는 대수만큼 동시에 돈다. 이체 대사·개인정보 파기·동의 만료 같은 배치가 겹쳐 돌면 같은 건을 두 번 처리한다.
 * 지금은 열두 개 배치가 모두 락을 갖고 있지만, 그것을 지켜주는 것이 아무것도 없다.
 *
 * <p>소스를 직접 읽는다. 스프링 컨텍스트를 띄우면 프로파일에 따라 빈이 없을 수 있어 "검사했는데 아무것도 못 찾은" 상태가 통과로 보인다.
 */
class ScheduledLockContractTests {

  private static final Path SOURCE_ROOT = Path.of("src/main/java");
  private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
  private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

  /**
   * 락이 필요 없는 예외.
   *
   * <p>Redis Stream 컨슈머 그룹은 인스턴스 분배를 자체로 한다. 여기에 락을 걸면 소비가 직렬화돼 오히려 처리량만 잃는다. 예외를 둘 때는 왜 안전한지 여기
   * 적는다.
   */
  private static final Set<String> LOCK_EXEMPT = Set.of("AsyncJobConsumer.java");

  @Test
  @DisplayName("@Scheduled 가 있으면 @SchedulerLock 도 있어야 한다")
  void everyScheduledMethodIsLocked() {
    List<String> offenders = new ArrayList<>();
    List<Path> scanned = new ArrayList<>();

    try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                String source = read(path);
                if (!source.contains("@Scheduled")) {
                  return;
                }
                scanned.add(path);
                if (LOCK_EXEMPT.contains(path.getFileName().toString())) {
                  return;
                }
                if (!source.contains("@SchedulerLock")) {
                  offenders.add(SOURCE_ROOT.relativize(path).toString());
                }
              });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // 아무것도 찾지 못한 채 통과하면 이 테스트는 아무것도 지키지 않는다.
    assertTrue(scanned.size() >= 10, "스케줄 클래스를 찾지 못했다. 경로가 바뀌었는지 확인해야 한다: " + scanned.size());

    assertTrue(
        offenders.isEmpty(), "@Scheduled 인데 @SchedulerLock 이 없다. 여러 인스턴스에서 중복 실행된다: " + offenders);
  }

  @Test
  @DisplayName("락 면제 목록에 적힌 파일은 실제로 존재해야 한다")
  void exemptionsStayAccurate() {
    // 파일이 사라지거나 이름이 바뀌면 면제가 조용히 무의미해진다. 그 상태로 두면
    // 다음 사람이 "예외가 이미 있으니 괜찮다" 고 읽는다.
    try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
      Set<String> present =
          files
              .map(path -> path.getFileName().toString())
              .collect(java.util.stream.Collectors.toSet());
      for (String exempt : LOCK_EXEMPT) {
        assertTrue(present.contains(exempt), "면제 목록에 있는 파일이 없다: " + exempt);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * 주석을 걷어낸 소스.
   *
   * <p>단순히 문자열을 찾으면 {@code // @SchedulerLock} 처럼 주석 처리된 애노테이션도 있는 것으로 센다. 락을 잠깐 꺼두고 되돌리지 않은 상태가 이
   * 검사를 그대로 통과하게 되므로, 실제로 지키려는 것을 못 지킨다.
   */
  private static String read(Path path) {
    try {
      String source = Files.readString(path);
      return BLOCK_COMMENT.matcher(LINE_COMMENT.matcher(source).replaceAll("")).replaceAll("");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
