package com.amlve.knowledge.service;

import com.amlve.knowledge.dto.response.ChapterResponse;

import java.util.List;

/**
 * 章节服务接口
 */
public interface KnowledgeChapterService {

    /**
     * 按课程查询章节列表（按 sortOrder 升序）
     *
     * @param courseId 课程ID
     * @return 章节列表
     */
    List<ChapterResponse> listByCourse(Long courseId);
}
