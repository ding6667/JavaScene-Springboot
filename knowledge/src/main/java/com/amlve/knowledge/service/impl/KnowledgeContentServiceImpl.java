package com.amlve.knowledge.service.impl;

import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.knowledge.Configuration;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.constant.KnowledgeConstant;
import com.amlve.knowledge.enumeration.ContentTypeEnum;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgeContentMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeContentService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgeContent;
import com.amlve.knowledge.tableEnt.KnowledgePoint;
import com.amlve.knowledgeapi.dto.request.KnowledgeContentQueryRequest;
import com.amlve.knowledgeapi.dto.request.KnowledgeContentSaveRequest;
import com.amlve.knowledgeapi.dto.response.KnowledgeContentVO;
import com.amlve.knowledgeapi.dto.response.KnowledgePointDTO;
import com.amlve.knowledgeapi.service.KnowledgeContentApi;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教材原文维护服务实现
 * <p>
 * 同时实现 {@link KnowledgeContentApi}，供备课、出题、批改智能体跨模块调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeContentServiceImpl implements KnowledgeContentService, KnowledgeContentApi {

    /**
     * 不分页取素材的单次上限：备课智能体一次拉整章，超过该值说明参数用错了
     */
    private static final int MATERIAL_QUERY_LIMIT = 200;

    /**
     * 默认内容类型（与表默认值一致：1=概念定义）
     */
    private static final int DEFAULT_CONTENT_TYPE = 1;

    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeChapterMapper chapterMapper;
    private final KnowledgePointMapper pointMapper;
    private final QAAgentClient qaAgentClient;
    private final Configuration configuration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveContent(KnowledgeContentSaveRequest request) {
        KnowledgeChapter chapter = requireChapter(request.getChapterId());
        requirePointInChapterIfPresent(request.getKnowledgePointId(), request.getChapterId());

        KnowledgeContent entity = request.getId() == null
                ? new KnowledgeContent() : contentMapper.selectById(request.getId());
        if (entity == null) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_CONTENT_NOT_FOUND);
        }
        entity.setChapterId(request.getChapterId());
        entity.setKnowledgePointId(request.getKnowledgePointId());
        entity.setContentType(request.getContentType() != null ? request.getContentType() : DEFAULT_CONTENT_TYPE);
        entity.setContent(request.getContent());
        entity.setPage(request.getPage());
        entity.setPageOrder(request.getPageOrder() != null ? request.getPageOrder() : 0);
        if (request.getId() == null) {
            contentMapper.insert(entity);
        } else {
            contentMapper.updateById(entity);
        }

        // Dify 同步失败只告警不回滚：本地素材是权威数据，知识库侧允许后续补偿重试
        if (!Boolean.FALSE.equals(request.getSyncDify())) {
            syncToDify(entity, chapter);
        }
        log.info("教材原文保存完成: id={}, chapterId={}, page={}, pageOrder={}",
                entity.getId(), entity.getChapterId(), entity.getPage(), entity.getPageOrder());
        return entity.getId();
    }

    /**
     * 把一条教材原文同步为 Dify 知识库文档并回写 documentId。
     * <p>
     * 文档名带记录ID，保证同一条素材重复保存时 Dify 命中同名幂等（更新而非新增副本）。
     */
    private void syncToDify(KnowledgeContent entity, KnowledgeChapter chapter) {
        String datasetId = requireDatasetId();
        String documentName = "教材原文-" + entity.getId();
        try {
            Map<String, Object> metadata = new HashMap<>();
            // 章节ID按 Long 原样传：主键已改雪花算法（19 位），intValue() 会静默溢出成乱值，
            // 而 Dify 工作流正是按 chapter_id 元数据过滤检索范围的——传错等于该章素材永远检索不到，
            // 且不会报错。与 KnowledgeDatasetServiceImpl 的上传通道保持同一口径
            metadata.put(KnowledgeConstant.METADATA_CHAPTER_ID, chapter.getId());
            metadata.put(KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
            if (entity.getPage() != null) {
                metadata.put(KnowledgeConstant.METADATA_PAGE, String.valueOf(entity.getPage()));
            }
            Result<DifyDocumentVO> created = qaAgentClient.createDocumentByText(
                    datasetId, documentName, entity.getContent());
            DifyDocumentVO document = created == null ? null : created.getData();
            if (document == null) {
                log.warn("Dify 同步未返回文档，本地已入库待补偿: contentId={}", entity.getId());
                return;
            }
            qaAgentClient.updateDocumentMetadata(datasetId, document.getDocumentId(), metadata);
            entity.setDifyDocumentId(document.getDocumentId());
            contentMapper.updateById(entity);
            log.info("教材原文已同步 Dify: contentId={}, documentId={}, totalSegments={}",
                    entity.getId(), document.getDocumentId(), document.getTotalSegments());
        } catch (Exception e) {
            log.warn("Dify 同步失败，本地已入库，等待补偿重试: contentId={}, chapterId={}, err={}",
                    entity.getId(), entity.getChapterId(), e.getMessage());
        }
    }

    @Override
    public Result<PageResult<KnowledgeContentVO>> listContents(KnowledgeContentQueryRequest request) {
        Page<KnowledgeContent> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<KnowledgeContent> pageData = contentMapper.selectPage(page,
                Wrappers.<KnowledgeContent>lambdaQuery()
                        .eq(KnowledgeContent::getChapterId, request.getChapterId())
                        .eq(request.getContentType() != null, KnowledgeContent::getContentType, request.getContentType())
                        .like(StringUtils.hasText(request.getKeyword()),
                                KnowledgeContent::getContent, request.getKeyword())
                        // page 已是 int 列，按数值序排序；旧版 varchar 会出现 "10" 排在 "2" 前的问题
                        .orderByAsc(KnowledgeContent::getPage)
                        .orderByAsc(KnowledgeContent::getPageOrder));
        PageResult<KnowledgeContentVO> result = new PageResult<>();
        result.setRecords(assembleVoList(pageData.getRecords()));
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return Result.success(result);
    }

    @Override
    public Result<List<KnowledgeContentVO>> listContentsByChapter(Long chapterId, Integer contentType) {
        requireChapter(chapterId);
        long total = contentMapper.selectCount(Wrappers.<KnowledgeContent>lambdaQuery()
                .eq(KnowledgeContent::getChapterId, chapterId)
                .eq(contentType != null, KnowledgeContent::getContentType, contentType));
        if (total > MATERIAL_QUERY_LIMIT) {
            throw new BusinessException("该章节素材共 " + total + " 条，超过单次上限 "
                    + MATERIAL_QUERY_LIMIT + " 条，请改用分页查询或指定内容类型");
        }
        List<KnowledgeContent> contents = contentMapper.selectList(Wrappers.<KnowledgeContent>lambdaQuery()
                .eq(KnowledgeContent::getChapterId, chapterId)
                .eq(contentType != null, KnowledgeContent::getContentType, contentType)
                .orderByAsc(KnowledgeContent::getPage)
                .orderByAsc(KnowledgeContent::getPageOrder));
        return Result.success(assembleVoList(contents));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContent(Long id) {
        KnowledgeContent entity = contentMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_CONTENT_NOT_FOUND);
        }
        contentMapper.deleteById(id);
        // Dify 侧归档删除失败不影响本地逻辑删除：孤儿文档不影响检索正确性，可由运维清理
        if (StringUtils.hasText(entity.getDifyDocumentId())) {
            try {
                qaAgentClient.deleteDocument(requireDatasetId(), entity.getDifyDocumentId());
            } catch (Exception e) {
                log.warn("Dify 文档删除失败: contentId={}, documentId={}, err={}",
                        id, entity.getDifyDocumentId(), e.getMessage());
            }
        }
        log.info("教材原文已逻辑删除: id={}, chapterId={}", id, entity.getChapterId());
    }

    @Override
    public Result<KnowledgePointDTO> getKnowledgePointById(Long pointId) {
        if (pointId == null) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_POINT_NOT_FOUND);
        }
        KnowledgePoint point = pointMapper.selectById(pointId);
        return Result.success(point == null ? null : toPointDto(point));
    }

    /**
     * 批量关联章节名与知识点名，避免逐条查询造成 N+1
     */
    private List<KnowledgeContentVO> assembleVoList(List<KnowledgeContent> contents) {
        if (contents.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> chapterIds = contents.stream().map(KnowledgeContent::getChapterId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> pointIds = contents.stream().map(KnowledgeContent::getKnowledgePointId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> chapterNames = chapterIds.isEmpty() ? Collections.emptyMap()
                : chapterMapper.selectBatchIds(chapterIds).stream()
                .collect(Collectors.toMap(KnowledgeChapter::getId, KnowledgeChapter::getTitle, (a, b) -> a));
        Map<Long, String> pointNames = pointIds.isEmpty() ? Collections.emptyMap()
                : pointMapper.selectBatchIds(pointIds).stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName, (a, b) -> a));

        List<KnowledgeContentVO> list = new ArrayList<>(contents.size());
        for (KnowledgeContent content : contents) {
            KnowledgeContentVO vo = new KnowledgeContentVO();
            vo.setId(content.getId());
            vo.setChapterId(content.getChapterId());
            vo.setChapterName(chapterNames.get(content.getChapterId()));
            vo.setKnowledgePointId(content.getKnowledgePointId());
            vo.setKnowledgePointName(pointNames.get(content.getKnowledgePointId()));
            vo.setContentType(content.getContentType());
            vo.setContentTypeName(ContentTypeEnum.descOf(content.getContentType()));
            vo.setContent(content.getContent());
            vo.setPage(content.getPage());
            vo.setPageOrder(content.getPageOrder());
            vo.setDifyDocumentId(content.getDifyDocumentId());
            vo.setCreateTime(content.getCreateTime());
            list.add(vo);
        }
        return list;
    }

    private KnowledgePointDTO toPointDto(KnowledgePoint point) {
        KnowledgePointDTO dto = new KnowledgePointDTO();
        dto.setId(point.getId());
        dto.setChapterId(point.getChapterId());
        dto.setName(point.getName());
        dto.setDescription(point.getDescription());
        dto.setDifficulty(point.getDifficulty());
        dto.setKeyword(point.getKeyword());
        dto.setPageRef(point.getPageRef());
        dto.setCreateTime(point.getCreateTime());
        dto.setUpdateTime(point.getUpdateTime());
        return dto;
    }

    private KnowledgeChapter requireChapter(Long chapterId) {
        if (chapterId == null) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        KnowledgeChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_NOT_FOUND);
        }
        return chapter;
    }

    /**
     * 关联知识点必须属于同一章节，否则出题按章节取素材时会串章
     */
    private void requirePointInChapterIfPresent(Long pointId, Long chapterId) {
        if (pointId == null) {
            return;
        }
        KnowledgePoint point = pointMapper.selectById(pointId);
        if (point == null) {
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_POINT_NOT_FOUND);
        }
        if (!chapterId.equals(point.getChapterId())) {
            throw new BusinessException(ErrorMessageConstant.WEIGHT_POINT_CHAPTER_MISMATCH);
        }
    }

    private String requireDatasetId() {
        String datasetId = configuration.getDifyDatasetId();
        if (!StringUtils.hasText(datasetId)) {
            throw new BusinessException(ErrorMessageConstant.DATASET_ID_MISSING);
        }
        return datasetId;
    }
}
