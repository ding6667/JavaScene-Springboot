package com.amlve.teachapi.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 作业题目快照的完整载荷（teach → exam，内部判题用）。
 * <p>
 * 与 {@link StudentHomeworkDetailResponse.StudentQuestionItem} 的区别：
 * 这里是「发布那一刻固化的完整题目」，包含标准答案、解析、测试用例与判题方式，
 * <b>只允许在服务端判题链路流转，任何面向学生的 VO 都不得引用本类字段。</b>
 * <p>
 * 用途：exam 侧归档错题时，用它构建「作业发布快照」（exam_question_snapshot 的 P: 版本），
 * 从而让错题重做按发布那一版的用例判题，而不是按题库最新用例。
 */
@Data
public class HomeworkQuestionSnapshotDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 作业题目快照ID（teach_homework_question.id） */
    private Long snapshotId;

    /** 题库题目ID（exam_question.id） */
    private Long questionId;

    private String content;

    private String shortTitle;

    /** 题型：1选择 2填空 3判断 4程序阅读 5编程 6简答 */
    private Integer type;

    /** 选项（选择题） */
    private List<String> options;

    /** 前端渲染配置（填空 blankCount、选择 choiceMode 等） */
    private JsonNode renderConfig;

    /** 编程题代码框架 */
    private String codeFramework;

    /** 作答要求 */
    private List<String> requirements;

    /** 阶梯提示 */
    private List<String> hints;

    /** 标准答案（题库原始 JSON 文本形态，如 {"correct":["A"]}） */
    private String answer;

    private String analysis;

    /** 解题思路 */
    private String solutionOutline;

    /** 测试用例（判题凭据，禁止下发） */
    private List<TestCaseItem> testCases;

    /** 判题方式：1=AI批改 2=测试用例判题 3=混合 */
    private Integer judgeMode;

    /** 题目分值（发布时的分值为准） */
    private Integer score;

    private Long chapterId;

    /** 知识点ID列表 */
    private List<Long> knowledgePointIds;

    /** 难度名称（发布时固化，题库改难度不影响） */
    private String difficultyName;

    @Data
    public static class TestCaseItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer caseIndex;
        private String input;
        private String expectedOutput;
        private Boolean isSample;
        private String caseType;
        private Integer timeLimit;
        private Integer memoryLimit;
    }
}
