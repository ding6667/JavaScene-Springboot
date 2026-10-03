package com.amlve.knowledgeapi.service;

import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.request.ChapterNamesRequest;
import com.amlve.knowledgeapi.dto.response.*;

import java.util.List;

/**
 * 章节与知识点对外服务接口（供 teach/study 等模块调用）
 * <p>
 * 跨模块只允许依赖 knowledge-api 的接口，禁止注入 knowledge 实现类。
 */
public interface KnowledgeChapterApi {

    /**
     * 按课程查询章节列表（按 sortOrder 升序）
     *
     * @param courseId 课程ID
     * @return 章节列表
     */
    Result<List<ChapterDTO>> listChaptersByCourse(Long courseId);

    /**
     * 按章节查询知识点列表
     *
     * @param chapterId 章节ID
     * @return 知识点列表
     */
    Result<List<KnowledgePointDTO>> listPointsByChapter(Long chapterId);

    /**
     * 根据章节ID获取全部七类备课素材
     *
     * @param chapterId 章节ID
     * @return 七类素材封装对象
     */
    Result<TeachingMaterialDTO> getTeachingMaterials(Long chapterId);



    Result<KnowledgeVerifyResponse> verify(KnowledgeVerifyRequest knowledgeVerifyRequest);

    Result<ChapterNameResponse> getChapterNameById(Long chapterId);


    Result<ChapterNamesResponse> getChapterNamesById(ChapterNamesRequest chapterIds);

    /**
     * 全量章节名称（结果可缓存，TTL ≥ 1 小时）
     */
    Result<List<ChapterNameResponse>> listAllChapterNames();

}
