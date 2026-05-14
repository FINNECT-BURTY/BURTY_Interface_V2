package com.berty.util;

/**
 * 한글 → 영문 키보드 자판 변환 유틸리티
 * 예: "홍길동" → "ghdrlfehd"
 */
public class KoreanKeyboardConverter {

    // 초성 19개 (ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ)
    private static final String[] CHOSUNG = {
        "r", "R", "s", "e", "E", "f", "a", "q", "Q", "t", "T", "d", "w", "W", "c", "z", "x", "v", "g"
    };

    // 중성 21개
    private static final String[] JUNGSUNG = {
        "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk", "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml", "l"
    };

    // 종성 28개 (0번 = 받침 없음)
    private static final String[] JONGSUNG = {
        "", "r", "R", "rt", "s", "sw", "sg", "e", "f", "fr", "fa", "fq", "ft", "fx", "fv", "fg",
        "a", "q", "qt", "t", "T", "d", "w", "c", "z", "x", "v", "g"
    };

    private KoreanKeyboardConverter() {}

    /**
     * 한글 문자열을 영문 키보드 자판 문자열로 변환합니다.
     * 한글 이외의 문자는 그대로 유지됩니다.
     */
    public static String convert(String input) {
        if (input == null) return null;
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int code = c - 0xAC00;
                int chosungIdx = code / (21 * 28);
                int jungsungIdx = (code % (21 * 28)) / 28;
                int jongsungIdx = code % 28;
                result.append(CHOSUNG[chosungIdx]);
                result.append(JUNGSUNG[jungsungIdx]);
                result.append(JONGSUNG[jongsungIdx]);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
