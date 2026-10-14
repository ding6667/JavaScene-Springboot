package com.amlve.knowledgeapi.service;

import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.request.ChapterNamesRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeVerifyRequest;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNamesResponse;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgeVerifyResponse;
import com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO;

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

    /**
     * AI 内容校验：判断智能体生成内容是否贴合该章教材
     *
     * @param request 校验请求（章节ID + 待校验内容）
     * @return 命中率、是否贴合、教材原文片段与修改建议
     */
    Result<KnowledgeVerifyResponse> verify(KnowledgeVerifyRequest request);

    /**
     * 查询单个章节名称
     *
     * @param chapterId 章节ID
     * @return 章节名称；章节不存在时 data 为 null，由调用方判断
     */
    Result<ChapterNameDTO> getChapterNameById(Long chapterId);

    /**
     * 批量查询章节名称（去重后最多 100 个）
     *
     * @param request 章节ID列表
     * @return 命中章节列表 + 未命中ID列表（个别未命中不整体报错）
     */
    Result<ChapterNamesResponse> getChapterNamesById(ChapterNamesRequest request);

    /**
     * 全量查询章节名称（章节基本不变，结果可缓存）
     *
     * @return 全部未删除章节的名称列表，按章节ID升序
     */
    Result<List<ChapterNameDTO>> listAllChapterNames();
}
