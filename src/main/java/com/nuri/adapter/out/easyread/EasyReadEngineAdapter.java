package com.nuri.adapter.out.easyread;

import com.nuri.application.port.out.EasyReadPort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EasyReadEngineAdapter implements EasyReadPort {
    private static final int MAX_SENTENCES = 3;
    private static final int MAX_SENTENCE_LENGTH = 40;
    private static final Pattern KRW_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*|\\d+)원");
    private static final Map<String, String> TERM_DICTIONARY = new LinkedHashMap<>();
    static {
        TERM_DICTIONARY.put("변동성", "출렁임");
        TERM_DICTIONARY.put("포트폴리오", "돈 묶음");
        TERM_DICTIONARY.put("샤프 지수", "안정도");
        TERM_DICTIONARY.put("리밸런싱", "다시 나누기");
        TERM_DICTIONARY.put("현금흐름", "들어오고 나가는 돈");
    }

    @Override
    public String toEasyRead(String rawText) {
        String converted = rawText == null ? "" : rawText;
        for (Map.Entry<String, String> e : TERM_DICTIONARY.entrySet()) converted = converted.replace(e.getKey(), e.getValue());
        converted = applyNumberAnalogy(converted);
        String[] split = converted.split("[.!?]");
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (String s : split) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            t = normalizeSentenceLength(t);
            out.append(t).append(". ");
            if (++count == MAX_SENTENCES) break;
        }
        return out.toString().trim();
    }

    @Override
    public String toSignalColor(double volatilityPercent) {
        if (volatilityPercent < 10) return "GREEN";
        if (volatilityPercent < 25) return "YELLOW";
        return "RED";
    }

    private String applyNumberAnalogy(String text) {
        Matcher matcher = KRW_PATTERN.matcher(text);
        StringBuffer converted = new StringBuffer();
        while (matcher.find()) {
            String source = matcher.group(1).replace(",", "");
            long amount;
            try {
                amount = Long.parseLong(source);
            } catch (NumberFormatException e) {
                continue;
            }
            long mealCount = Math.max(1L, amount / 12000L);
            matcher.appendReplacement(converted, matcher.group(1) + "원(점심 약 " + mealCount + "번)");
        }
        matcher.appendTail(converted);
        return converted.toString();
    }

    private String normalizeSentenceLength(String sentence) {
        if (sentence.length() <= MAX_SENTENCE_LENGTH) {
            return sentence;
        }
        int pivot = sentence.lastIndexOf(' ', MAX_SENTENCE_LENGTH);
        if (pivot < 10) {
            pivot = MAX_SENTENCE_LENGTH;
        }
        return sentence.substring(0, pivot).trim();
    }
}
