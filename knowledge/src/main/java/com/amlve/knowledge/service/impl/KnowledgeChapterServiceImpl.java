package com.amlve.knowledge.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.converter.KnowledgeConverter;
import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeChapterService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

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
    private final KnowledgeConverter knowledgeConverter;

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
