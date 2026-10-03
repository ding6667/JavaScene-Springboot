package com.amlve.knowledgeapi.dto.response;

public class ChapterNameResponse {

    private Long chapterId;
    private String chapterName;   // 章节全称，如 "第2章 Java 编程基础"*
    private String shortName;     // 短名，如 "Java 编程基础"（去掉"第X章 "前缀）*
}
