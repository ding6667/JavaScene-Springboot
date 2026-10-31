package com.amlve.exam.util;

import com.amlve.common.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * exam_question.render_config（前端渲染配置）的规范化与兜底推断。
 * <p>
 * 该列是自由 JSON 对象，按题型定形：
 * <ul>
 *   <li>选择 {@code {"choiceMode":"single"}} / {@code {"choiceMode":"multiple"}}</li>
 *   <li>填空 {@code {"blankCount":3}}</li>
 *   <li>编程 {@code {"language":"java"}}（可选）</li>
 *   <li>其它题型可为空</li>
 * </ul>
 * 未识别的额外键一律保留，便于前端自行扩展渲染参数。
 * <p>
 * 写入侧（管理端保存、AI 出题落库）走 {@link #toStored}：能推就推，推不出就留空；
 * 回显侧（管理端/学生端）走 {@link #forRender}：选择题、填空题保证有可用值，
 * 存量老数据（该列为 NULL）也能直接渲染，前端不必再按题型做降级判断。
 */
public final class QuestionRenderConfigCodec {

    /** 选择：单选 / 多选 */
    public static final String KEY_CHOICE_MODE = "choiceMode";
    /** 填空：需要留几个空 */
    public static final String KEY_BLANK_COUNT = "blankCount";
    /** 编程：语言，缺省按 java */
    public static final String KEY_LANGUAGE = "language";

    public static final String CHOICE_SINGLE = "single";
    public static final String CHOICE_MULTIPLE = "multiple";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 题干里的空位：连续两个及以上下划线 */
    private static final Pattern UNDERSCORE_BLANK = Pattern.compile("_{2,}");
    /** 题干里的空位：全角括号内留白，如「（  ）」；代码里的 () 不算，避免程序阅读题误判 */
    private static final Pattern PAREN_BLANK = Pattern.compile("[（(]\\s+[)）]");

    private QuestionRenderConfigCodec() {
    }

    /**
     * 写入侧：前端/AI 传入的渲染配置 → 可直接入库的 JSON 对象。
     *
     * @param config  渲染配置（对象、JSON 文本、null 都接受）
     * @param type    题型，决定要补齐哪个键
     * @param content 题干，缺省时用于推断填空空数
     * @param answer  结构化答案，缺省时用于推断单选/多选
     * @return 规范化后的对象；无任何可用键时返回 null（列留空，由回显侧兜底）
     */
    public static JsonNode toStored(JsonNode config, QuestionTypeEnum type, String content, JsonNode answer) {
        ObjectNode node = normalize(config, type, content, answer);
        return node.isEmpty() ? null : node;
    }

    /**
     * 回显侧：保证选择题有 choiceMode、填空题有 blankCount，其余题型原样返回。
     *
     * @return 渲染配置；其它题型且没有任何键时返回 null
     */
    public static JsonNode forRender(JsonNode config, QuestionTypeEnum type, String content, JsonNode answer) {
        return toStored(config, type, content, answer);
    }

    /**
     * 填空空数：配置优先，其次按题干空位统计，最后兜底 1。
     */
    public static int blankCount(JsonNode config, String content) {
        Integer configured = intOrNull(config == null ? null : config.path(KEY_BLANK_COUNT));
        if (configured != null && configured > 0) {
            return configured;
        }
        int counted = countBlanks(content);
        return counted > 0 ? counted : 1;
    }

    /**
     * 选择模式：配置优先，其次按答案个数推断（多于一个即多选），最后兜底单选。
     */
    public static String choiceMode(JsonNode config, JsonNode answer) {
        String configured = normalizeChoiceMode(config == null ? null : config.path(KEY_CHOICE_MODE));
        if (configured != null) {
            return configured;
        }
        return answerChoiceCount(answer) > 1 ? CHOICE_MULTIPLE : CHOICE_SINGLE;
    }

    // ==================== 内部实现 ====================

    private static ObjectNode normalize(JsonNode config, QuestionTypeEnum type, String content, JsonNode answer) {
        ObjectNode node = toObject(config);
        if (type == null) {
            return node;
        }
        switch (type) {
            case CHOICE -> node.put(KEY_CHOICE_MODE, choiceMode(node, answer));
            case FILL_IN -> node.put(KEY_BLANK_COUNT, blankCount(node, content));
            default -> {
                // 判断/程序阅读/编程/简答：不补键，前端按题型默认渲染；编程可带 language
            }
        }
        return node;
    }

    /** 各种入参形态收敛成可变对象：对象深拷贝、JSON 文本解析、其余忽略 */
    private static ObjectNode toObject(JsonNode config) {
        if (config == null || config.isNull() || config.isMissingNode()) {
            return JsonNodeFactory.instance.objectNode();
        }
        if (config.isObject()) {
            return ((ObjectNode) config).deepCopy();
        }
        if (config.isTextual()) {
            String text = config.asText().trim();
            if (text.isEmpty()) {
                return JsonNodeFactory.instance.objectNode();
            }
            try {
                JsonNode parsed = MAPPER.readTree(text);
                if (parsed != null && parsed.isObject()) {
                    return ((ObjectNode) parsed).deepCopy();
                }
            } catch (Exception ignored) {
                // 非法 JSON 文本直接忽略，不阻断保存
            }
        }
        return JsonNodeFactory.instance.objectNode();
    }

    /** 单选/多选写法收敛：单选 single、多选 multiple，其它写法返回 null 交给推断 */
    private static String normalizeChoiceMode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText("").trim().toLowerCase();
        return switch (text) {
            case "single", "single_choice", "radio", "单选", "1", "true" -> CHOICE_SINGLE;
            case "multiple", "multi", "multi_choice", "multiple_choice", "checkbox", "多选", "2", "false" -> CHOICE_MULTIPLE;
            default -> null;
        };
    }

    /** 答案个数：结构化数组取长度，扁平串按分隔符切分 */
    private static int answerChoiceCount(JsonNode answer) {
        if (answer == null || answer.isNull() || answer.isMissingNode()) {
            return 0;
        }
        JsonNode correct = answer.isObject() ? answer.path("correct") : answer;
        if (correct == null || correct.isNull() || correct.isMissingNode()) {
            return 0;
        }
        if (correct.isArray()) {
            int count = 0;
            for (JsonNode item : correct) {
                if (!item.asText("").isBlank()) {
                    count++;
                }
            }
            return count;
        }
        if (correct.isBoolean()) {
            return 1;
        }
        String text = correct.asText("").trim();
        if (text.isEmpty()) {
            return 0;
        }
        // 兼容扁平写法：A|C、A,C、A C、AC
        int split = text.split("[\\s,，;；|/]+").length;
        return Math.max(split, text.replaceAll("\\s+", "").length());
    }

    private static int countBlanks(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return count(UNDERSCORE_BLANK, content) + count(PAREN_BLANK, content);
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        try {
            return Integer.parseInt(node.asText("").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
