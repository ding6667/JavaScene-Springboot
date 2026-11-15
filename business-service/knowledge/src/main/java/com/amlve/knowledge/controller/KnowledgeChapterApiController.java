package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.service.impl.KnowledgeChapterServiceImpl;
import com.amlve.knowledgeapi.dto.request.ChapterNamesRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeVerifyRequest;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.dto.response.ChapterNamesResponse;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgeVerifyResponse;
import com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * KnowledgeChapterApi 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/knowledge/chapter")
public class KnowledgeChapterApiController implements KnowledgeChapterApi {

    private final KnowledgeChapterServiceImpl knowledgeChapterApi;

    @Override
    public Result<List<ChapterDTO>> listChaptersByCourse(Long courseId) {
        return knowledgeChapterApi.listChaptersByCourse(courseId);
    }

    @Override
    public Result<List<KnowledgePointDTO>> listPointsByChapter(Long chapterId) {
        return knowledgeChapterApi.listPointsByChapter(chapterId);
    }

    @Override
    public Result<TeachingMaterialDTO> getTeachingMaterials(Long chapterId) {
        return knowledgeChapterApi.getTeachingMaterials(chapterId);
    }

    @Override
    public Result<KnowledgeVerifyResponse> verify(KnowledgeVerifyRequest request) {
        return knowledgeChapterApi.verify(request);
    }

    @Override
    public Result<ChapterNameDTO> getChapterNameById(Long chapterId) {
        return knowledgeChapterApi.getChapterNameById(chapterId);
    }

    @Override
    public Result<ChapterNamesResponse> getChapterNamesById(ChapterNamesRequest request) {
        return knowledgeChapterApi.getChapterNamesById(request);
    }

    @Override
    public Result<List<ChapterNameDTO>> listAllChapterNames() {
        return knowledgeChapterApi.listAllChapterNames();
    }
}
