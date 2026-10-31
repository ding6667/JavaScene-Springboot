package com.amlve.exam.util;

import com.amlve.common.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 答案 JSON ↔ 扁平文本 互转测试，样例取自 exam_question 库内真实形状。
 */
class QuestionAnswerCodecTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void toText_shouldFlattenEveryStoredShape() {
        assertThat(QuestionAnswerCodec.toText(json("{\"correct\":[\"B\"]}"))).isEqualTo("B");
        assertThat(QuestionAnswerCodec.toText(json("{\"correct\":[\"A\",\"C\"]}"))).isEqualTo("A|C");
        assertThat(QuestionAnswerCodec.toText(json("{\"blanks\":[\"length()\",\"equals()\"]}")))
                .isEqualTo("length()|equals()");
        assertThat(QuestionAnswerCodec.toText(json("{\"correct\":true}"))).isEqualTo("true");
        assertThat(QuestionAnswerCodec.toText(json("{\"output\":\"9\"}"))).isEqualTo("9");
        assertThat(QuestionAnswerCodec.toText(json("{\"reference\":\"return n;\"}"))).isEqualTo("return n;");
    }

    @Test
    void toText_shouldPassThroughLegacyTextValue_andNull() {
        // 早期直接存文本的行：json 列是个带引号的字符串
        assertThat(QuestionAnswerCodec.toText(TextNode.valueOf("A"))).isEqualTo("A");
        assertThat(QuestionAnswerCodec.toText(null)).isNull();
        assertThat(QuestionAnswerCodec.toText(json("null"))).isNull();
    }

    @Test
    void toJson_shouldShapeByQuestionType() {
        assertThat(QuestionAnswerCodec.toJson("A", QuestionTypeEnum.CHOICE).toString())
                .isEqualTo("{\"correct\":[\"A\"]}");
        assertThat(QuestionAnswerCodec.toJson("A,C", QuestionTypeEnum.CHOICE).toString())
                .isEqualTo("{\"correct\":[\"A\",\"C\"]}");
        assertThat(QuestionAnswerCodec.toJson("对", QuestionTypeEnum.JUDGE).toString())
                .isEqualTo("{\"correct\":true}");
        assertThat(QuestionAnswerCodec.toJson("false", QuestionTypeEnum.JUDGE).toString())
                .isEqualTo("{\"correct\":false}");
        assertThat(QuestionAnswerCodec.toJson("9", QuestionTypeEnum.PROGRAM_READ).toString())
                .isEqualTo("{\"output\":\"9\"}");
        assertThat(QuestionAnswerCodec.toJson("return n;", QuestionTypeEnum.PROGRAMMING).toString())
                .isEqualTo("{\"reference\":\"return n;\"}");
    }

    @Test
    void toJson_shouldKeepInnerCommas_whenFillIn() {
        // 填空各空之间才用 | 切，空内自带的逗号不能切掉
        assertThat(QuestionAnswerCodec.toJson("int a, b|equals()", QuestionTypeEnum.FILL_IN).toString())
                .isEqualTo("{\"blanks\":[\"int a, b\",\"equals()\"]}");
    }

    @Test
    void toJson_shouldNotFabricateBoolean_whenJudgeAnswerUnreadable() {
        assertThat(QuestionAnswerCodec.toJson("看情况", QuestionTypeEnum.JUDGE).toString())
                .isEqualTo("{\"reference\":\"看情况\"}");
    }

    @Test
    void roundTrip_shouldSurviveForObjectiveTypes() {
        for (QuestionTypeEnum type : new QuestionTypeEnum[]{
                QuestionTypeEnum.CHOICE, QuestionTypeEnum.FILL_IN, QuestionTypeEnum.JUDGE}) {
            String flat = type == QuestionTypeEnum.JUDGE ? "对"
                    : type == QuestionTypeEnum.CHOICE ? "A|C" : "length()|equals()";
            String expected = type == QuestionTypeEnum.JUDGE ? "true" : flat;
            assertThat(QuestionAnswerCodec.toText(QuestionAnswerCodec.toJson(flat, type)))
                    .as("round trip of %s", type)
                    .isEqualTo(expected);
        }
    }
}
