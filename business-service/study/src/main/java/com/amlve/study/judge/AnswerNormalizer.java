package com.amlve.study.judge;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 答案归一化：把「语义相同、写法不同」的答案收敛成同一个字符串。
 *
 * <p>铁律：比较必须「两侧同时归一化」，只对一侧处理 = 变相放宽/收紧标准答案。
 */
public final class AnswerNormalizer {

    private AnswerNormalizer() {}

    private static final Set<String> TRUE_WORDS  = Set.of("对", "正确", "是", "√", "true", "t", "1", "yes", "y");
    private static final Set<String> FALSE_WORDS = Set.of("错", "错误", "否", "×", "x", "false", "f", "0", "no", "n");

    /** NFKC 覆盖不到的中文标点单独映射（、。《》「」——…） */
    private static final String[][] PUNCT = {
            {"、", ","}, {"。", "."}, {"《", "<"}, {"》", ">"},
            {"「", "\""}, {"」", "\""}, {"『", "\""}, {"』", "\""},
            {"——", "-"}, {"—", "-"}, {"…", "..."}
    };

    /** 基础归一化：NFKC(全角→半角/①→1) + 中文标点 + 空白压缩 + 转小写。用于选项文本、判断题 */
    public static String basic(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        for (String[] p : PUNCT) s = s.replace(p[0], p[1]);
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** 文本归一化：basic + 去全部空白 + 去结尾句号。用于填空题 */
    public static String text(String raw) {
        String s = basic(raw).replaceAll("\\s+", "");
        return s.replaceAll("[.。]+$", "");
    }

    /**
     * 多行归一化：统一换行 + 逐行【只去行尾空白】+ 丢空行 + 转小写。用于程序阅读题输出。
     * 注意：不 stripLeading —— 金字塔/对齐类输出的行首空格是有意义的。
     */
    public static String multiline(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .replace("\r\n", "\n").replace('\r', '\n');
        for (String[] p : PUNCT) s = s.replace(p[0], p[1]);
        List<String> lines = new ArrayList<>();
        for (String line : s.split("\n", -1)) {
            String t = line.stripTrailing().toLowerCase(Locale.ROOT);
            if (!t.isBlank()) lines.add(t);
        }
        return String.join("\n", lines);
    }

    /** 压成单行，兜底「学生把多行输出挤成一行」 */
    public static String compact(String raw) {
        return basic(raw).replaceAll("\\s+", "");
    }

    /** 判断题归一成布尔；无法识别返回 null（标准答案解析不出必须报错，不能静默判错） */
    public static Boolean toBoolean(String raw) {
        String s = text(raw);
        if (TRUE_WORDS.contains(s))  return Boolean.TRUE;
        if (FALSE_WORDS.contains(s)) return Boolean.FALSE;
        return null;
    }
}
