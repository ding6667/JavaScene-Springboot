package com.amlve.exam.util;

import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.teachapi.dto.response.HomeworkQuestionSnapshotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 题库题目 → 错题快照两个 JSON 包的编解码。
 * <p>
 * 拆包的目的不是省字段，而是「物理隔离」：
 * {@link #viewOf} 产出的包可以直接塞进学生端 VO，
 * {@link #judgeOf} 产出的包含标准答案、解析与测试用例，永远不下发；
 * 两者不放在同一个对象里，就不会出现「加字段时忘了排除」的泄露。
 * <p>
 * 指纹 {@link #fingerprint} 只覆盖会影响作答与判分的三要素（题干、选项、标准答案），
 * 这三者任一变化都算题目被修订，需要新开一条错题记录。
 */
public final class QuestionSnapshotCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private QuestionSnapshotCodec() {
    }

    /**
     * 内容指纹：题干 + 选项 + 标准答案的 MD5（十六进制，32 位）。
     * <p>
     * 用内容算而不是靠题库加版本号：直接改库、脚本刷数据也能识别出「题变了」。
     */
    public static String fingerprint(ExamQuestion question) {
        if (question == null) {
            return "";
        }
        return fingerprintOf(question.getContent(), question.getOptions(),
                QuestionAnswerCodec.toText(question.getAnswer()));
    }

    /**
     * 内容指纹的通用形态：三段原文拼接后取 MD5。
     * 作业发布快照用它记录「发布那一版」的内容指纹（该快照不参与过期判定，只作留痕）。
     *
     * @param content     题干
     * @param optionsText 选项原文（JSON 文本或纯文本均可，只要同源一致）
     * @param answerText  标准答案扁平文本
     */
    public static String fingerprintOf(String content, String optionsText, String answerText) {
        String raw = nullToEmpty(content) + "|" + nullToEmpty(optionsText) + "|" + nullToEmpty(answerText);
        return md5(raw);
    }

    /** 选项列表 → 指纹用的稳定文本：能序列化成 JSON 就用 JSON，失败退回竖线拼接 */
    public static String optionsText(List<String> options) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(options);
        } catch (Exception e) {
            return String.join("|", options);
        }
    }

    /**
     * 学生可见渲染包：题干、选项、渲染配置、代码框架、作答要求、提示等。
     * 不含 answer / analysis / testCases。
     */
    public static ObjectNode viewOf(ExamQuestion question) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (question == null) {
            return node;
        }
        node.put("content", question.getContent());
        node.put("shortTitle", question.getShortTitle());
        node.put("type", question.getType() == null ? null : question.getType().getCode());
        node.set("options", parseOptions(question.getOptions()));
        if (question.getRenderConfig() != null) {
            node.set("renderConfig", question.getRenderConfig());
        }
        node.put("codeFramework", question.getCodeFramework());
        node.set("requirements", toArray(question.getRequirements()));
        node.set("hints", toArray(question.getHints()));
        node.put("score", question.getScore());
        node.put("chapterId", question.getChapterId());
        node.set("knowledgePoints", toArray(question.getKnowledgePointIds()));
        return node;
    }

    /**
     * 判题与解析包：标准答案、解析、解题思路、测试用例、判题方式。
     * 只允许服务端判题链路读取。
     */
    public static ObjectNode judgeOf(ExamQuestion question) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (question == null) {
            return node;
        }
        if (question.getAnswer() != null) {
            node.set("answer", question.getAnswer());
        }
        node.put("analysis", question.getAnalysis());
        node.put("solutionOutline", question.getSolutionOutline());
        node.set("testCases", MAPPER.valueToTree(
                question.getTestCases() == null ? List.of() : question.getTestCases()));
        node.put("judgeMode", question.getJudgeMode());
        return node;
    }

    /**
     * 学生可见渲染包（作业发布快照版）：数据来自 teach 发布时固化的那份快照，
     * 而不是题库当前内容 —— 这正是「作业题不会因题库修订而变」的落地点。
     *
     * @param snapshot teach 作业题目快照载荷
     */
    public static ObjectNode viewOf(HomeworkQuestionSnapshotDTO snapshot) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (snapshot == null) {
            return node;
        }
        node.put("content", snapshot.getContent());
        node.put("shortTitle", snapshot.getShortTitle());
        node.put("type", snapshot.getType());
        node.set("options", toArray(snapshot.getOptions()));
        if (snapshot.getRenderConfig() != null) {
            node.set("renderConfig", snapshot.getRenderConfig());
        }
        node.put("codeFramework", snapshot.getCodeFramework());
        node.set("requirements", toArray(snapshot.getRequirements()));
        node.set("hints", toArray(snapshot.getHints()));
        node.put("score", snapshot.getScore());
        node.put("chapterId", snapshot.getChapterId());
        node.set("knowledgePoints", toArray(snapshot.getKnowledgePointIds()));
        return node;
    }

    /**
     * 判题与解析包（作业发布快照版）：答案/用例都取发布那一版，
     * 因此错题重做不会因为题库改了用例而判错。
     *
     * @param snapshot teach 作业题目快照载荷（含答案与用例，禁止下发学生端）
     */
    public static ObjectNode judgeOf(HomeworkQuestionSnapshotDTO snapshot) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (snapshot == null) {
            return node;
        }
        node.set("answer", parseAnswer(snapshot.getAnswer()));
        node.put("analysis", snapshot.getAnalysis());
        node.put("solutionOutline", snapshot.getSolutionOutline());
        node.set("testCases", MAPPER.valueToTree(
                snapshot.getTestCases() == null ? List.of() : snapshot.getTestCases()));
        node.put("judgeMode", snapshot.getJudgeMode());
        return node;
    }

    // ==================== 读包 ====================
    public static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public static Integer integer(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asInt();
    }

    public static Long longValue(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() || !value.isNumber() ? null : value.asLong();
    }

    public static JsonNode node(JsonNode node, String field) {
        return node == null ? null : node.get(field);
    }

    /** 数组包 → List&lt;String&gt;；非数组统一返回 null，由调用方决定是否兜底 */
    public static List<String> stringList(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isArray()) {
            return null;
        }
        return MAPPER.convertValue(value, MAPPER.getTypeFactory()
                .constructCollectionType(List.class, String.class));
    }

    public static List<Long> longList(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isArray()) {
            return null;
        }
        return MAPPER.convertValue(value, MAPPER.getTypeFactory()
                .constructCollectionType(List.class, Long.class));
    }

    /** 对象数组包（如 testCases）→ List，元素不符时返回空列表 */
    public static <T> List<T> objectList(JsonNode node, String field, Class<T> type) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        return MAPPER.convertValue(value, MAPPER.getTypeFactory().constructCollectionType(List.class, type));
    }

    // ==================== 内部 ====================

    /**
     * 答案原文（题库/作业快照里都是 JSON 文本，如 {@code {"correct":["A"]}}）→ JsonNode。
     * 解析不了就按纯文本存，至少不丢内容。
     */
    private static JsonNode parseAnswer(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            return JsonNodeFactory.instance.nullNode();
        }
        try {
            JsonNode parsed = MAPPER.readTree(answerText);
            return parsed == null ? JsonNodeFactory.instance.textNode(answerText) : parsed;
        } catch (Exception e) {
            return JsonNodeFactory.instance.textNode(answerText);
        }
    }

    /**
     * options 在题库里是 JSON 数组字符串，早期也有直接存纯文本的：
     * 能解析成数组就存数组，解析不了就原样存字符串，至少不丢信息。
     */
    private static JsonNode parseOptions(String options) {
        if (options == null || options.isBlank()) {
            return JsonNodeFactory.instance.arrayNode();
        }
        try {
            JsonNode parsed = MAPPER.readTree(options);
            return parsed == null ? JsonNodeFactory.instance.arrayNode() : parsed;
        } catch (Exception e) {
            return JsonNodeFactory.instance.textNode(options);
        }
    }

    private static ArrayNode toArray(List<?> values) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        if (values == null) {
            return array;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                array.add(number.longValue());
            } else {
                array.add(String.valueOf(value));
            }
        }
        return array;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String md5(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 是 JDK 必备算法，走不到这里
            throw new IllegalStateException("MD5 不可用", e);
        }
    }
}
