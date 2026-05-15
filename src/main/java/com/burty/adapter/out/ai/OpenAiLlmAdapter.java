package com.burty.adapter.out.ai;

import com.burty.application.port.out.LlmPort;
import com.burty.config.AiProperties;
import com.burty.domain.entity.AiFallbackTemplateEntity;
import com.burty.domain.repository.AiFallbackTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiLlmAdapter implements LlmPort {
    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmAdapter.class);

    private final RestTemplate restTemplate;
    private final AiProperties properties;
    private final AiFallbackTemplateRepository templateRepository;

    public OpenAiLlmAdapter(RestTemplate restTemplate, AiProperties properties, AiFallbackTemplateRepository templateRepository) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.templateRepository = templateRepository;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (properties.isStubMode() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            return contextualFallback(userPrompt);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            Map response = restTemplate.postForObject(
                    properties.getBaseUrl() + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            String text = extractText(response);
            return (text == null || text.isBlank()) ? contextualFallback(userPrompt) : text;
        } catch (Exception e) {
            log.warn("OpenAI call failed, returning contextual fallback err={}", e.getMessage());
            return contextualFallback(userPrompt);
        }
    }

    private String contextualFallback(String userPrompt) {
        FallbackContext ctx = FallbackContext.from(userPrompt);
        String managed = managedTemplate(ctx);
        if (managed != null && !managed.isBlank()) {
            return managed
                    .replace("{riskDate}", ctx.riskDateText())
                    .replace("{balance}", ctx.balanceText());
        }

        if (ctx.red() && ctx.freelancer()) {
            return "수입 변동이 큰 상태에서 " + ctx.riskDateText() + " 잔액이 " + ctx.balanceText() + "까지 내려갈 수 있어요. "
                    + "이번 달 들어올 돈은 생활비와 고정비 계좌로 먼저 나누는 게 우선이에요. "
                    + "오늘 비상금 5만원과 월세·카드 결제금액을 먼저 분리해주세요.";
        }
        if (ctx.red() && ctx.jobSeeker()) {
            return ctx.riskDateText() + "에 잔액 부족 위험이 있어요. "
                    + "소비를 더 줄이는 것만으로는 한계가 있으니 정책 지원 확인이 먼저예요. "
                    + "오늘 정책 매칭에서 긴급생활비나 주거 지원 신청 가능 항목 1개를 확인해주세요.";
        }
        if (ctx.red() && ctx.rentRelated()) {
            return "월세나 고정비가 먼저 빠지면서 " + ctx.riskDateText() + " 잔액이 마이너스가 될 수 있어요. "
                    + "이번 위험은 소액 절약보다 납부일과 입금일 충돌을 푸는 게 효과적이에요. "
                    + "오늘 월세·관리비 납부일과 급여일을 한 화면에서 다시 맞춰주세요.";
        }
        if (ctx.red() && ctx.cardRelated()) {
            return "카드 결제 이후 잔액이 " + ctx.balanceText() + " 수준까지 떨어질 수 있어요. "
                    + "이번 달은 추가 소비보다 카드 자동결제와 결제일 확인이 먼저예요. "
                    + "오늘 카드 자동결제 항목 중 금액이 큰 1건을 보류하거나 결제일 변경 가능 여부를 확인해주세요.";
        }
        if (ctx.red() && ctx.loanRelated()) {
            return "대출 상환일 전후로 잔액 부족 위험이 커 보여요. "
                    + "상환 조건을 임의로 바꾸기보다 고금리·필수 상환 항목을 먼저 확인해야 해요. "
                    + "오늘 대출 상환 목록에서 금리와 납부일이 높은 항목 1개를 확인해주세요.";
        }
        if (ctx.red()) {
            return ctx.riskDateText() + "에 잔액이 " + ctx.balanceText() + "까지 내려갈 위험이 있어요. "
                    + "이번 달은 새 지출을 줄이고 고정비 빠지는 순서를 먼저 확인해야 해요. "
                    + "오늘 위험일 전까지 나갈 자동이체 1건을 점검해주세요.";
        }

        if (ctx.yellow() && ctx.freelancer()) {
            return "월말 잔액이 안전선에 가까워지고, 수입 변동성도 함께 보여요. "
                    + "이번 달은 많이 번 날의 소비를 바로 늘리지 않는 게 중요해요. "
                    + "오늘 생활비 예산을 먼저 분리하고 남은 금액 안에서만 추가 소비를 정해주세요.";
        }
        if (ctx.yellow() && ctx.cardRelated()) {
            return "카드 결제 후 잔액이 안전잔액 아래로 내려갈 가능성이 있어요. "
                    + "전체 소비를 크게 줄이기보다 자동결제와 식비처럼 바로 조정 가능한 항목부터 보는 게 좋아요. "
                    + "오늘 카드 자동결제 항목 1건과 이번 주 식비 예산을 같이 점검해주세요.";
        }
        if (ctx.yellow() && ctx.rentRelated()) {
            return "월세 이후 잔액 여유가 얇아질 수 있어요. "
                    + "월세는 피하기 어려우니 그 전후 7일의 변동지출을 낮추는 방식이 현실적이에요. "
                    + "월세 납부 전까지 배달·카페 예산을 5만원만 줄여주세요.";
        }
        if (ctx.yellow() && ctx.jobSeeker()) {
            return "큰 적자는 아니지만 생활비 여유가 충분하지는 않아요. "
                    + "구직 기간에는 절약과 함께 받을 수 있는 지원을 같이 확인해야 버틸 수 있어요. "
                    + "오늘 정책 매칭에서 생활비 또는 교통비 지원 항목을 1개 확인해주세요.";
        }
        if (ctx.yellow()) {
            return "월말 잔액이 안전선 아래로 내려갈 가능성이 있어요. "
                    + "이번 주에 조정 가능한 식비·구독·자동결제부터 줄이면 위험을 낮출 수 있어요. "
                    + "오늘 가장 큰 변동지출 1개를 골라 이번 주 예산에서 제외해주세요.";
        }

        if (ctx.freelancer()) {
            return "현재 흐름은 급한 위험은 낮지만 수입 변동이 있는 편이에요. "
                    + "수입이 들어온 달에 다음 달 생활비를 먼저 떼어두면 불안이 줄어요. "
                    + "오늘 다음 달 고정비 1주치만 별도 계좌로 분리해주세요.";
        }
        if (ctx.jobSeeker()) {
            return "현재 흐름은 비교적 안정적이지만 구직 기간에는 예상 밖 지출에 약할 수 있어요. "
                    + "정책 지원과 최소 생활비 기준을 같이 잡아두는 게 좋아요. "
                    + "오늘 정책 매칭에서 신청 가능한 항목 1개를 저장해주세요.";
        }
        if (ctx.highSpend()) {
            return "위험 단계는 높지 않지만 월지출 규모가 커서 작은 변동에도 여유가 줄 수 있어요. "
                    + "고정비보다 먼저 줄일 수 있는 반복 지출을 찾는 게 현실적이에요. "
                    + "오늘 구독·보험·통신비 중 하나를 골라 필요 여부를 확인해주세요.";
        }
        return "이번 달 흐름은 안정적이에요. 큰 변동은 없을 전망이에요. "
                + "이번 주 안에 자동이체 내역 한 번 점검해주세요.";
    }

    private String managedTemplate(FallbackContext ctx) {
        for (AiFallbackTemplateEntity template : templateRepository.findByActiveTrue()) {
            if (matches(template.getRiskLevel(), ctx.level())
                    && matches(template.getOccupationCode(), ctx.occupation())
                    && matches(template.getCauseType(), ctx.causeType())) {
                return template.getTemplateText();
            }
        }
        return null;
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(actual);
    }

    private record FallbackContext(String raw, String level, long projectedBalance, String riskDate,
                                   long monthlySpend, String reason) {
        static FallbackContext from(String prompt) {
            String raw = prompt == null ? "" : prompt;
            return new FallbackContext(
                    raw,
                    extractRiskLevel(raw),
                    extractMoneyAfter(raw, "최소 잔액:"),
                    extractTextAfter(raw, "위험일:"),
                    extractMoneyAfter(raw, "월지출:"),
                    extractTextAfter(raw, "사유:")
            );
        }

        boolean red() { return "RED".equals(level) || raw.contains("level\":\"RED"); }
        boolean yellow() { return "YELLOW".equals(level) || raw.contains("level\":\"YELLOW"); }
        boolean freelancer() { return raw.contains("FREELANCER"); }
        boolean jobSeeker() { return raw.contains("JOB_SEEKER"); }
        boolean rentRelated() { return containsAny(reason, "월세", "관리비", "고정지출", "고정비"); }
        boolean cardRelated() { return containsAny(reason, "카드", "자동결제", "결제일"); }
        boolean loanRelated() { return containsAny(reason, "대출", "상환", "이자"); }
        boolean highSpend() { return monthlySpend >= 2_500_000L; }
        String occupation() {
            if (freelancer()) return "FREELANCER";
            if (jobSeeker()) return "JOB_SEEKER";
            return "-";
        }
        String causeType() {
            if (rentRelated()) return "RENT";
            if (cardRelated()) return "CARD_BILL";
            if (loanRelated()) return "LOAN";
            return "-";
        }

        String riskDateText() {
            return (riskDate == null || riskDate.isBlank() || "없음".equals(riskDate)) ? "30일 안" : riskDate;
        }

        String balanceText() {
            return String.format("%,d원", projectedBalance);
        }

        private static String extractRiskLevel(String raw) {
            if (raw.contains("위험단계: RED")) return "RED";
            if (raw.contains("위험단계: YELLOW")) return "YELLOW";
            if (raw.contains("위험단계: GREEN")) return "GREEN";
            return "-";
        }

        private static long extractMoneyAfter(String raw, String marker) {
            String text = extractTextAfter(raw, marker);
            if (text == null) return 0L;
            String normalized = text.replace(",", "").replace("원", "").trim();
            StringBuilder number = new StringBuilder();
            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                if ((c == '-' && number.isEmpty()) || Character.isDigit(c)) {
                    number.append(c);
                } else if (!number.isEmpty()) {
                    break;
                }
            }
            if (number.isEmpty() || "-".contentEquals(number)) return 0L;
            try {
                return Long.parseLong(number.toString());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }

        private static String extractTextAfter(String raw, String marker) {
            int start = raw.indexOf(marker);
            if (start < 0) return "";
            int valueStart = start + marker.length();
            int end = raw.indexOf('\n', valueStart);
            String value = end < 0 ? raw.substring(valueStart) : raw.substring(valueStart, end);
            return value.trim();
        }

        private static boolean containsAny(String value, String... needles) {
            if (value == null) return false;
            for (String needle : needles) {
                if (value.contains(needle)) return true;
            }
            return false;
        }
    }

    private String extractText(Map response) {
        if (response == null) return "";
        Object choicesObj = response.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "";
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return "";
        Object msgObj = firstMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) return "";
        Object content = msgMap.get("content");
        return content == null ? "" : String.valueOf(content);
    }
}
