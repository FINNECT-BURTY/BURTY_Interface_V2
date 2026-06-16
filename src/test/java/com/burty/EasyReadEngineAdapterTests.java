/**
 *
 *
 * <pre>
 * <b>Description  : [테스트] 공통 통합 테스트 (EasyReadEngineAdapterTests)</b>
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

import com.burty.adapter.out.easyread.EasyReadEngineAdapter;
import com.burty.application.port.in.admin.BaseCodeUseCase;
import com.burty.domain.admin.entity.BaseCodeEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EasyReadEngineAdapterTests {

  private final BaseCodeUseCase emptyLookup =
      new BaseCodeUseCase() {
        @Override
        public List<BaseCodeEntity> lookup(String codeGroup) {
          return List.of();
        }

        @Override
        public Optional<BaseCodeEntity> lookup(String codeGroup, String codeValue) {
          return Optional.empty();
        }

        @Override
        public List<BaseCodeEntity> children(String parentCodeId) {
          return List.of();
        }

        @Override
        public String displayName(String codeGroup, String codeValue, String localeTag) {
          return codeValue;
        }

        @Override
        public BaseCodeEntity upsert(BaseCodeEntity entity, String operator) {
          return entity;
        }

        @Override
        public void deactivate(String codeId, String operator) {}

        @Override
        public void reload() {}
      };

  private final EasyReadEngineAdapter adapter = new EasyReadEngineAdapter(emptyLookup);

  @Test
  void toEasyRead_appliesDictionaryAndSentenceLimits() {
    String raw =
        "포트폴리오 변동성 200000원 샤프 지수 점검 필요합니다. "
            + "두 번째 문장은 아주 길어서 마흔 글자를 넘어가도록 일부러 작성했습니다. "
            + "세 번째 문장입니다. 네 번째 문장은 잘려야 합니다.";

    String converted = adapter.toEasyRead(raw);

    Assertions.assertTrue(converted.contains("돈 묶음"));
    Assertions.assertTrue(converted.contains("출렁임"));
    Assertions.assertTrue(converted.contains("안정도"));
    Assertions.assertTrue(converted.contains("점심 약"));
    Assertions.assertTrue(converted.split("\\.").length <= 3);
  }

  @Test
  void signalColor_matchesPolicy() {
    Assertions.assertEquals("GREEN", adapter.toSignalColor(5.0));
    Assertions.assertEquals("YELLOW", adapter.toSignalColor(15.0));
    Assertions.assertEquals("RED", adapter.toSignalColor(30.0));
  }
}
