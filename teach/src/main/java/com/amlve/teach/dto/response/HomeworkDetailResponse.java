package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师端作业详情响应：作业元信息 + 题目完整快照（含答案与解析，仅教师可见）。
 * 提交人数等统计字段待 teach_homework 补 submitted_count 列后提供。
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
        private Integer type;
        private String typeName;
        /** 选项 JSON 数组字符串（原始格式，前端自行解析） */
        private String options;
        /** 标准答案 JSON 字符串 */
        private String answer;
        private String analysis;
        private Integer difficulty;
        private Long chapterId;
        /** 知识点 JSON 数组字符串 */
        private String knowledgePoints;
        private Integer defaultScore;
        private Integer page;
        private Integer score;
        private Integer sortOrder;
    }
}
