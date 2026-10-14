package com.amlve.knowledge.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgePointService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledgeapi.dto.request.KnowledgePointSaveRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识点维护服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    /**
     * 默认难度（与表默认值一致：1=了解）
     */
    private static final int DEFAULT_DIFFICULTY = 1;

    private final KnowledgePointMapper pointMapper;

    private final KnowledgeChapterMapper chapterMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePoint(KnowledgePointSaveRequest request) {
        KnowledgeChapter chapter = chapterMapper.selectById(request.getChapterId());
        if (chapter == null) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_NOT_FOUND);
        }
        KnowledgePoint entity = request.getId() == null
                ? new KnowledgePoint() : pointMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_POINT_NOT_FOUND);
        }
        entity.setChapterId(request.getChapterId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setDifficulty(request.getDifficulty() != null ? request.getDifficulty() : DEFAULT_DIFFICULTY);
        entity.setKeyword(request.getKeyword());
        entity.setPageRef(request.getPageRef());
        entity.setPageOrder(request.getPageOrder() != null ? request.getPageOrder() : 0);
        if (request.getId() == null) {
            pointMapper.insert(entity);
        } else {
            pointMapper.updateById(entity);
        }
        log.info("知识点保存完成: id={}, chapterId={}, name={}", entity.getId(), entity.getChapterId(), entity.getName());
        return entity.getId();
    }
}
