package com.amlve.teach.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师端作业详情响应：作业元信息 + 题目完整快照（含答案与解析，仅教师可见）。
 * 提交数量统计来自 teach_homework.submitted_count 列（提交回调自增）。
 */
@Data
public class HomeworkDetailResponse {

    private Long id;
    private String title;
    private String content;
    private Long chapterId;
    private Long classId;
    private String grade;
    private Integer status;
    /** 状态名（已发布/已截止等） */
    private String statusName;
    private Integer totalScore;
    private Integer questionCount;

    /** 提交数量（学生提交回调自增） */
    private Integer submittedCount;
    private Integer allowLateSubmit;
    private LocalDateTime deadline;
    private LocalDateTime publishTime;
    private LocalDateTime closeTime;
    private String closeReason;
    private LocalDateTime createTime;

    /** 题目快照列表（按 sortOrder 升序） */
    private List<QuestionItem> questions;

    /**
     * 教师端题目项：完整快照，含答案、解析
     */
    @Data
    public static class QuestionItem {
        private Long id;
        private Long questionId;
        private Integer sourceType;
        private String content;
        /** 简短题目快照（一句话概括题意） */
        private String shortTitle;
        /** 前端渲染配置快照（JSON 对象）：填空 blankCount、选择 choiceMode 等 */
        private JsonNode renderConfig;
        private Integer type;
        private String typeName;
        /** 选项 JSON 数组字符串（原始格式，前端自行解析） */
        private String options;
        /** 标准答案 JSON 字符串 */
        private String answer;
        private String analysis;
        /** 编程题代码框架快照（发布时固化） */
        private String codeFramework;
        /** 作答要求快照（JSON 数组） */
        private JsonNode requirements;
        /** 阶梯提示快照（JSON 数组） */
        private JsonNode hints;
        /** 解题思路快照 */
        private String solutionOutline;
        /** 判题方式快照：1=AI批改 2=测试用例判题 3=混合 */
        private Integer judgeMode;
        private String difficulty;
        private Long chapterId;
        /** 知识点 JSON 数组字符串 */
        private String knowledgePoints;
        private Integer defaultScore;
        private Integer page;
        private Integer score;
        private Integer sortOrder;
    }
}
