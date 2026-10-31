package com.amlve.exam.util;

import com.amlve.common.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * exam_question.answer 的两种形态互转。
 * <p>
 * 库里 answer 是 json 列，按题型存结构化对象：
 * 选择 {@code {"correct":["A","B"]}}、填空 {@code {"blanks":["a","b"]}}、
 * 判断 {@code {"correct":true}}、程序阅读 {@code {"output":"9"}}、编程/简答 {@code {"reference":"..."}}。
 * <p>
 * 但 exam-api 的 DTO 与判分链路（study 的 JudgeService、评分智能体）都按扁平字符串消费：
 * 选择题 A/B 键、填空题各空按 {@code |} 分隔、判断题 true/false。
 * 转换集中在这里，避免每个调用方各写一套导致口径漂移。
 */
public class QuestionAnswerCodec {

    /** 多空/多选分隔符：study 的 JudgeService 按 | 切分标准答案的各空 */
    private static final String SEPARATOR = "|";

    /** 选择题：A,B / A;B / A B 等写法都收敛成数组元素 */
    private static final Pattern CHOICE_SPLIT = Pattern.compile("[\\s,，;；|]+");

    /** 填空题只认 | 分隔：空内可能自带逗号（如 int a, b），不能按逗号切 */
    private static final Pattern BLANK_SPLIT = Pattern.compile("\\s*\\|\\s*");

    private static final Set<String> TRUE_WORDS = Set.of("对", "正确", "是", "√", "true", "t", "1", "yes", "y");
    private static final Set<String> FALSE_WORDS = Set.of("错", "错误", "否", "×", "x", "false", "f", "0", "no", "n");

    private QuestionAnswerCodec() {
    }

    /**
     * 库里的结构化答案 → 扁平文本（exam-api DTO 与判分链路消费的形态）。
     *
     * @param answer 库内 JsonNode 答案
     * @return 扁平答案；answer 为 null 时返回 null
     */
    public static String toText(JsonNode answer) {
        if (answer == null || answer.isNull() || answer.isMissingNode()) {
            return null;
        }
        // 兼容早期直接存文本的行：json 列里就是个带引号的字符串
        if (answer.isTextual()) {
            return answer.asText();
        }
        if (answer.isArray()) {
            return join(answer);
        }
        if (answer.isObject()) {
            JsonNode correct = answer.path("correct");
            if (hasValue(correct)) {
                return correct.isBoolean() ? String.valueOf(correct.asBoolean()) : joinOrText(correct);
            }
            for (String key : new String[]{"blanks", "output", "reference", "answer"}) {
                JsonNode node = answer.path(key);
                if (hasValue(node)) {
                    return joinOrText(node);
                }
            }
        }
        // 没见过的形状原样吐 JSON，至少不丢信息
        return answer.toString();
    }

    /**
     * 扁平文本 → 库里的结构化答案，JSON 形状由题型决定。
     *
     * @param answer 扁平答案（管理端保存、出题智能体产出的都是这种）
     * @param type   题型；为空时退化为文本节点
     * @return 可直接写入 json 列的 JsonNode
     */
    public static JsonNode toJson(String answer, QuestionTypeEnum type) {
        String text = answer == null ? "" : answer.trim();
        if (type == null) {
            return TextNode.valueOf(text);
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        switch (type) {
            case CHOICE -> node.set("correct", toArray(text, CHOICE_SPLIT));
            case FILL_IN -> node.set("blanks", toArray(text, BLANK_SPLIT));
            case JUDGE -> {
                Boolean value = toBoolean(text);
                if (value == null) {
                    // 判断不出来就不硬编成布尔，保留原文供人工核对
                    node.put("reference", text);
                } else {
                    node.put("correct", value);
                }
            }
            case PROGRAM_READ -> node.put("output", text);
            default -> node.put("reference", text);
        }
        return node;
    }

    /**
     * 前端传来的答案（对象 / 数组 / 纯文本）→ 可直接写入 json 列的结构化答案。
     * <p>
     * 管理端接口已改为直接传对象，这里只做三件事：空值归一化成 null、纯文本按题型转结构、
     * 其它形状（对象、数组）原样入库——避免后端再猜一遍形状反而把前端的数据改坏。
     *
     * @param answer 前端传入的答案节点
     * @param type   题型；纯文本转结构时使用
     * @return 用于入库的 JsonNode；答案为空时返回 null
     */
    public static JsonNode toStored(JsonNode answer, QuestionTypeEnum type) {
        if (isEmpty(answer)) {
            return null;
        }
        if (answer.isTextual()) {
            return toJson(answer.asText(), type);
        }
        return answer;
    }

    /**
     * 答案是否为空：null、JSON null、空串/空白串、空对象、空数组都算空。
     */
    public static boolean isEmpty(JsonNode answer) {
        if (answer == null || answer.isNull() || answer.isMissingNode()) {
            return true;
        }
        if (answer.isTextual()) {
            return answer.asText().isBlank();
        }
        return answer.isContainerNode() && answer.isEmpty();
    }

    private static boolean hasValue(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull();
    }

    private static String  joinOrText(JsonNode node) {
        return node.isArray() ? join(node) : node.asText();
    }

    private static String join(JsonNode array) {
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        for (JsonNode item : array) {
            joiner.add(item.asText());
        }
        return joiner.toString();
    }

    private static ArrayNode toArray(String text, Pattern separator) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        if (text.isEmpty()) {
            return array;
        }
        for (String part : separator.split(text)) {
            if (!part.isBlank()) {
                array.add(part.trim());
            }
        }
        return array;
    }

    private static Boolean toBoolean(String text) {
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (TRUE_WORDS.contains(normalized)) {
            return Boolean.TRUE;
        }
        if (FALSE_WORDS.contains(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
