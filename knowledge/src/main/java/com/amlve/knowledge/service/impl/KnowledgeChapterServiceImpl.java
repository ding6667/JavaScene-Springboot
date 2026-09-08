package com.amlve.knowledge.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.converter.KnowledgeConverter;
import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgeCodeSnippetMapper;
import com.amlve.knowledge.mapper.KnowledgeConceptMapper;
import com.amlve.knowledge.mapper.KnowledgeDifficultPointMapper;
import com.amlve.knowledge.mapper.KnowledgeExerciseMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeChapterService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgeCodeSnippet;
import com.amlve.knowledge.tableEnt.KnowledgeConcept;
import com.amlve.knowledge.tableEnt.KnowledgeDifficultPoint;
import com.amlve.knowledge.tableEnt.KnowledgeExercise;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节服务实现
 * <p>
 * 同时实现 {@link com.amlve.knowledgeapi.service.KnowledgeChapterApi}，
 * 供其他模块通过 knowledge-api 接口调用（跨模块禁止直接注入本实现类）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeChapterServiceImpl implements KnowledgeChapterService,
        com.amlve.knowledgeapi.service.KnowledgeChapterApi {

    private final KnowledgeChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final KnowledgeConceptMapper conceptMapper;
    private final KnowledgeCodeSnippetMapper codeSnippetMapper;
    private final KnowledgeDifficultPointMapper difficultPointMapper;
    private final KnowledgeExerciseMapper exerciseMapper;
    private final KnowledgeConverter knowledgeConverter;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChapterResponse> listByCourse(Long courseId) {
        return knowledgeConverter.toResponseList(selectChaptersByCourse(courseId));
    }

    @Override
    public List<ChapterDTO> listChaptersByCourse(Long courseId) {
        return knowledgeConverter.toChapterDTOList(selectChaptersByCourse(courseId));
    }

    @Override
    public List<KnowledgePointDTO> listPointsByChapter(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        List<KnowledgePoint> points = pointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getChapterId, chapterId)
        );
        log.info("查询章节知识点: chapterId={}, 命中{}条", chapterId, points.size());
        return knowledgeConverter.toKnowledgePointDTOList(points);
    }

    @Override
    public TeachingMaterialDTO getTeachingMaterials(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }

        KnowledgeChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在: chapterId=" + chapterId);
        }

        String chapterSummary = chapter.getDescription();

        String conceptDefinitions = buildConceptDefinitionsJson(chapterId);
        String codeExamples = buildCodeExamplesJson(chapterId);
        String commonMistakes = buildCommonMistakesJson(chapterId);
        String pageReferences = buildPageReferencesJson(chapterId);
        String exercises = buildExercisesJson(chapterId);
        String labTasks = "[]";

        log.info("查询章节备课素材: chapterId={}, title={}", chapterId, chapter.getTitle());
        return TeachingMaterialDTO.builder()
                .chapterSummary(chapterSummary)
                .conceptDefinitions(conceptDefinitions)
                .codeExamples(codeExamples)
                .commonMistakes(commonMistakes)
                .pageReferences(pageReferences)
                .exercises(exercises)
                .labTasks(labTasks)
                .build();
    }

    private String buildConceptDefinitionsJson(Long chapterId) {
        List<KnowledgeConcept> concepts = conceptMapper.selectList(
                new LambdaQueryWrapper<KnowledgeConcept>()
                        .eq(KnowledgeConcept::getChapterId, chapterId)
                        .orderByAsc(KnowledgeConcept::getPage)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeConcept c : concepts) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", c.getName());
            map.put("definition", c.getDefinition());
            map.put("difficulty", c.getDifficulty());
            map.put("page", c.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildCodeExamplesJson(Long chapterId) {
        List<KnowledgeCodeSnippet> snippets = codeSnippetMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCodeSnippet>()
                        .eq(KnowledgeCodeSnippet::getChapterId, chapterId)
                        .orderByAsc(KnowledgeCodeSnippet::getPage)
                        .orderByAsc(KnowledgeCodeSnippet::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeCodeSnippet s : snippets) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", s.getTitle());
            map.put("code", s.getCode());
            map.put("isKeyExample", s.getIsKeyExample());
            map.put("page", s.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildCommonMistakesJson(Long chapterId) {
        List<KnowledgeDifficultPoint> points = difficultPointMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDifficultPoint>()
                        .eq(KnowledgeDifficultPoint::getChapterId, chapterId)
                        .orderByAsc(KnowledgeDifficultPoint::getPage)
                        .orderByAsc(KnowledgeDifficultPoint::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeDifficultPoint p : points) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("description", p.getDescription());
            map.put("type", p.getType());
            map.put("page", p.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String buildPageReferencesJson(Long chapterId) {
        List<KnowledgePoint> points = pointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getChapterId, chapterId)
        );
        Map<String, String> map = new LinkedHashMap<>();
        for (KnowledgePoint p : points) {
            map.put(p.getName(), p.getPageRef());
        }
        return toJson(map);
    }

    private String buildExercisesJson(Long chapterId) {
        List<KnowledgeExercise> exerciseList = exerciseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeExercise>()
                        .eq(KnowledgeExercise::getChapterId, chapterId)
                        .orderByAsc(KnowledgeExercise::getPage)
                        .orderByAsc(KnowledgeExercise::getPageOrder)
        );
        List<Map<String, Object>> list = new ArrayList<>();
        for (KnowledgeExercise e : exerciseList) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("question", e.getQuestion());
            map.put("answer", e.getAnswer());
            map.put("difficulty", e.getDifficulty());
            map.put("knowledgePoint", e.getKnowledgePoint());
            map.put("page", e.getPage());
            list.add(map);
        }
        return toJson(list);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "[]";
        }
    }

    private List<KnowledgeChapter> selectChaptersByCourse(Long courseId) {
        if (ObjectUtils.isEmpty(courseId)) {
            throw new BusinessException(ErrorMessageConstant.COURSE_ID_EMPTY);
        }
        List<KnowledgeChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChapter>()
                        .eq(KnowledgeChapter::getCourseId, courseId)
                        .orderByAsc(KnowledgeChapter::getSortOrder)
        );
        log.info("查询章节列表: courseId={}, 命中{}条", courseId, chapters.size());
        return chapters;
    }
}