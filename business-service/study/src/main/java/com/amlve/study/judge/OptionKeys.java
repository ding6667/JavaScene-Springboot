package com.amlve.study.judge;

import java.util.*;
import java.util.regex.*;

/** 选择题选项键解析：AnswerValidator 与 ObjectiveJudgeService 共用一套规则 */
public final class OptionKeys {

    private OptionKeys() {}

    private static final Pattern OPTION_PREFIX = Pattern.compile("^\\s*([A-Za-z])[.、):：\\s]");

    /** 合法选项键：优先取选项自带前缀(A. B.)，缺失时按位置兜底 A/B/C/D */
    public static Set<Character> legalKeys(List<String> options) {
        Set<Character> keys = new LinkedHashSet<>();
        if (options == null) return keys;
        for (String option : options) {
            Matcher m = OPTION_PREFIX.matcher(option == null ? "" : option);
            if (m.find()) keys.add(Character.toUpperCase(m.group(1).charAt(0)));
        }
        if (keys.size() == options.size()) return keys;
        keys.clear();
        for (int i = 0; i < options.size() && i < 26; i++) keys.add((char) ('A' + i));
        return keys;
    }

    /** 解析 "A" / "AB" / "A,B" / "A. 多态" 中的键 */
    public static Set<Character> parse(String answer) {
        Set<Character> picked = new LinkedHashSet<>();
        if (answer == null) return picked;
        Matcher prefix = OPTION_PREFIX.matcher(answer.trim());
        if (prefix.find()) {
            picked.add(Character.toUpperCase(prefix.group(1).charAt(0)));
            return picked;
        }
        for (char c : answer.replaceAll("[\\s,，;；/|]", "").toCharArray()) {
            if (Character.isLetter(c)) picked.add(Character.toUpperCase(c));
        }
        return picked;
    }

    /** 解析不到键时，用选项文本反查（学生直接填了选项内容） */
    public static Set<Character> resolve(String answer, List<String> options) {
        Set<Character> keys = parse(answer);
        if (!keys.isEmpty() || options == null || options.isEmpty()) return keys;
        String target = AnswerNormalizer.text(answer);
        Set<Character> legal = legalKeys(options);
        int i = 0;
        for (String option : options) {
            if (i >= 26) break;
            char key = (char) ('A' + i++);
            if (!legal.contains(key)) continue;
            String optText = AnswerNormalizer.text(stripPrefix(option));
            if (!optText.isEmpty() && (optText.equals(target) || target.contains(optText))) {
                keys.add(key);
            }
        }
        return keys;
    }

    private static String stripPrefix(String option) {
        Matcher m = OPTION_PREFIX.matcher(option == null ? "" : option);
        return m.find() ? option.substring(m.end()) : option;
    }

    public static String keysToString(Set<Character> keys) {
        return keys.stream().sorted().map(String::valueOf).reduce((a, b) -> a + b).orElse("");
    }
}
