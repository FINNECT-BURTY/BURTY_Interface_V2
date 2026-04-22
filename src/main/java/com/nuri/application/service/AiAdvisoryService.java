package com.nuri.application.service;

import com.nuri.application.port.in.AiAdvisoryUseCase;
import com.nuri.application.port.out.EasyReadPort;
import com.nuri.application.port.out.LlmPort;
import com.nuri.application.port.out.MyDataPort;
import com.nuri.domain.model.AssetSnapshot;
import com.nuri.domain.model.ConsultationResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAdvisoryService implements AiAdvisoryUseCase {
    private final MyDataPort myDataPort;
    private final LlmPort llmPort;
    private final EasyReadPort easyReadPort;

    public AiAdvisoryService(MyDataPort myDataPort, LlmPort llmPort, EasyReadPort easyReadPort) {
        this.myDataPort = myDataPort;
        this.llmPort = llmPort;
        this.easyReadPort = easyReadPort;
    }

    @Override
    public ConsultationResult consultWithAi(String userId, String question) {
        AssetSnapshot snapshot = myDataPort.fetchAssetSnapshot(userId);
        String systemPrompt = """
                당신은 55-65세 사용자를 돕는 시니어 친화 재무 비서다.
                답변 규칙:
                1) 최대 3문장
                2) 전문용어 최소화
                3) 마지막 문장은 지금 할 일 1개를 안내
                """;
        String userPrompt = """
                사용자 질문: %s
                현재 자산정보:
                - 총자산: %.0f원
                - 월지출: %.0f원
                - 변동성: %.1f%%
                """.formatted(question, snapshot.getTotalAsset(), snapshot.getMonthlySpend(), snapshot.getVolatilityPercent());

        String llmAnswer = llmPort.generate(systemPrompt, userPrompt);
        String easySummary = easyReadPort.toEasyRead(llmAnswer);
        return new ConsultationResult(
                easySummary,
                easyReadPort.toSignalColor(snapshot.getVolatilityPercent()),
                List.of("지출 점검", "가족 알림 확인", "질문 다시하기")
        );
    }
}
