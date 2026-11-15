package com.amlve.exam.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题重做页面视图（学生端）。
 * <p>
 * 只由快照的「学生可见渲染包」组装：不含标准答案、解析、测试用例。
 * 判题包由服务端内部按 snapshotId 取，学生端拿不到。
 */
@Data
public class WrongQuestionRetryVO {

    /** 错题记录ID */
    private Long wrongQuestionId;

    /** 题目快照ID */
    private Long snapshotId;

    /** 题库题目ID */
    private Long questionId;

    /** 原错题来源：1=作业，2=刷题；重做提交时原样回传 */
    private Integer source;

    /** 快照来源：1=题库版本，2=作业发布快照 */
    private Integer snapshotType;

    /** 题干 */
    private String content;

    /** 简短题目 */
    private String shortTitle;

    /** 题型 */
    private Integer type;

    /** 选项（选择题） */
    private List<String> options;

    /** 前端渲染配置：填空 blankCount、选择 choiceMode 等 */
    private JsonNode renderConfig;

    /** 编程题代码框架 */
    private String codeFramework;

    /** 主观题作答要求 */
    private List<String> requirements;

    /** 阶梯提示 */
    private List<String> hints;

    /** 知识点ID */
    private List<Long> knowledgePointIds;

    /** 分值 */
    private Integer score;

    /** 章节ID */
    private Long chapterId;

    /** 上次错误答案 */
    private String lastAnswer;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 是否已掌握：0=未掌握，1=已掌握 */
    private Integer mastered;

    /**
     * 题面是否已过期：题库来源的错题，题库把题改了会置 true。
     * 作业来源恒为 false（作业就该按发布那一版看）。
     */
    private Boolean outdated;

    /** 最近答错时间 */
    private LocalDateTime lastWrongTime;
}
