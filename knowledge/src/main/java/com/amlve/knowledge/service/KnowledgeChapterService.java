package com.amlve.knowledge.service;

import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledgeapi.dto.request.KnowledgeChapterSaveRequest;
import com.amlve.knowledgeapi.dto.response.ChapterTreeDTO;

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

    /**
     * 按课程查询章节树（一次查全量后在内存按 parentId 组装，不递归查库）
     *
     * @param courseId 课程ID
     * @return 顶级章节列表，子章节挂在 children 上
     */
    List<ChapterTreeDTO> chapterTree(Long courseId);

    /**
     * 新增或更新章节（id 为空则新增）
     *
     * @param request 章节内容
     * @return 章节ID
     */
    Long saveChapter(KnowledgeChapterSaveRequest request);
}
