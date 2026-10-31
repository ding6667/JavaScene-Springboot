package com.amlve.examapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 错题重做载荷（exam-api，供 study 判题使用）。
 * <p>
 * <b>含标准答案与测试用例，禁止直接下发学生端。</b>
 * 学生端要展示的是 {@code /api/exam/wrong-question/{id}/retry} 返回的视图，
 * 那个接口只取本对象的 view 部分。
 * <p>
 * 为什么要按快照判题：学生重做的是「当时做错的那一版题」，
 * 如果按题库最新版判，题改过之后旧代码可能永远过不了，会出现「按原来那版写对了却判错」。
 */
@Data
public class WrongQuestionRetryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 错题记录ID */
    private Long wrongQuestionId;

    /** 题目快照ID（exam_question_snapshot.id） */
    private Long snapshotId;

    /** 题库题目ID */
    private Long questionId;

    /** 原错题来源：1=作业，2=刷题。重做后仍按原来源归档，避免凭空多出一条错题 */
    private Integer source;

    /** 快照来源：1=题库版本，2=作业发布快照 */
    private Integer snapshotType;

    // ==================== 作答视图（可下发） ====================

    private String content;
    private String shortTitle;
    private Integer type;
    private List<String> options;
    private JsonNode renderConfig;
    private String codeFramework;
    private List<String> requirements;
    private List<String> hints;
    private List<Long> knowledgePointIds;
    private Integer score;
    private Long chapterId;

    /** 上次错误答案，用于重做时回填对照 */
    private String lastAnswer;

    // ==================== 判题包（禁止下发） ====================

    /** 标准答案（快照时的扁平文本） */
    private String answer;

    private String analysis;

    private String solutionOutline;

    /** 判题方式：1=AI批改，2=测试用例判题，3=混合 */
    private Integer judgeMode;

    /** 测试用例（隐藏用例也在内，禁止下发） */
    private List<TestCaseItem> testCases;

    @Data
    public static class TestCaseItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer caseIndex;
        private String input;
        private String expectedOutput;
        private Boolean isSample;
        private String caseType;
        private Integer weight;
        private Integer timeLimit;
        private Integer memoryLimit;
        private String description;
    }
}
