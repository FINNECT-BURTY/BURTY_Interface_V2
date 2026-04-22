package com.nuri;

import com.nuri.application.port.out.EasyReadPort;
import com.nuri.application.port.out.LlmPort;
import com.nuri.application.port.out.MyDataPort;
import com.nuri.application.service.AiAdvisoryService;
import com.nuri.domain.model.AssetSnapshot;
import com.nuri.domain.model.ConsultationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAdvisoryServiceTests {

    @Test
    void consultWithAi_returnsEasyReadResultWithSignal() {
        MyDataPort myDataPort = userId -> new AssetSnapshot(100_000_000, 2_000_000, 12.5);
        LlmPort llmPort = (systemPrompt, userPrompt) ->
                "포트폴리오 상태가 보통이에요. 이번 달 지출이 커졌어요. 자동이체 점검하세요.";
        EasyReadPort easyReadPort = new EasyReadPort() {
            @Override
            public String toEasyRead(String rawText) {
                return rawText;
            }

            @Override
            public String toSignalColor(double volatilityPercent) {
                return "YELLOW";
            }
        };
        AiAdvisoryService service = new AiAdvisoryService(myDataPort, llmPort, easyReadPort);

        ConsultationResult result = service.consultWithAi("u1", "이번달 괜찮아?");

        Assertions.assertEquals("YELLOW", result.getSignalColor());
        Assertions.assertTrue(result.getSummary().contains("자동이체"));
        Assertions.assertFalse(result.getRecommendedActions().isEmpty());
    }
}
