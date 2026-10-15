package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

/**
 * 章节名称（供智能体与教师侧回填出处，避免各模块自己解析"第X章"前缀）
 */
@Data
public class ChapterNameDTO {

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 章节全称，如 "第 2 章 Java 编程基础"
     */
    private String chapterName;

    /**
     * 短名，去掉"第X章 "前缀后的部分，如 "Java 编程基础"；无前缀时与全称相同
     */
    private String shortName;
}
