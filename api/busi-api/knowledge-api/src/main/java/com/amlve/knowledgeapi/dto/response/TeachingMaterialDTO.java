package com.amlve.knowledgeapi.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TeachingMaterialDTO {

    /**
     * 章节总结
     */
    private String chapterSummary;

    /**
     * 概念定义（JSON 字符串）
     */
    private String conceptDefinitions;

    /**
     * 代码示例（JSON 字符串）
     */
    private String codeExamples;

    /**
     * 常见易错点（JSON 字符串）
     */
    private String commonMistakes;

    /**
     * 教材页码参考（JSON 字符串）
     */
    private String pageReferences;

    /**
     * 课后习题（JSON 字符串）
     */
    private String exercises;

    /**
     * 上机实训任务（JSON 字符串）
     */
    private String labTasks;
}