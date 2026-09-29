package com.amlve.knowledge.service.impl;

import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledge.Configuration;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.constant.KnowledgeConstant;
import com.amlve.knowledge.converter.KnowledgeConverter;
import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import com.amlve.knowledge.dto.response.VerifyResponse;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgeContentMapper;
import com.amlve.knowledge.mapper.KnowledgeKeywordMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeDatasetService;
import com.amlve.knowledge.service.KnowledgeWeightConfigService;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgeContent;
import com.amlve.knowledge.tableEnt.KnowledgeKeyword;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import com.amlve.systemapi.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库数据集服务实现
 * <p>
 * 同时实现 {@link com.amlve.knowledgeapi.service.KnowledgeService}，
 * 供其他模块通过 knowledge-api 接口调用（跨模块禁止直接注入本实现类）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDatasetServiceImpl implements KnowledgeDatasetService,
        com.amlve.knowledgeapi.service.KnowledgeService {

    private final Configuration configuration;
    private final QAAgentClient qaAgentClient;
    private final KnowledgeChapterMapper chapterMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeKeywordMapper keywordMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeConverter knowledgeConverter;
    private final UserService userService;
    private final KnowledgeWeightConfigService weightConfigService;

    @Override
    public KnowledgeResponse query(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            throw new BusinessException(ErrorMessageConstant.KEYWORDS_EMPTY);
        }
        KnowledgeResponse response = doQuery(keywords);
        if (response.getItems().isEmpty()) {
            response.setMessage(ErrorMessageConstant.KNOWLEDGE_NOT_FOUND);
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResponse uploadText(UploadTextRequest request) {
        KnowledgeChapter chapter = requireChapter(request.getChapterId());
        log.info("上传文本文档: chapterId={}, documentName={}", request.getChapterId(), request.getDocumentName());

        // 1. Dify 创建文本文档（返回 documentId；createChapterDocument 返回 void，故不用）
        //TODO 需增加请求失败时的分支处理
        String documentId = qaAgentClient.createDocumentByText(
                requireDatasetId(), request.getDocumentName(), request.getContent()).getData();

        // 2. 写入元数据：chapter_id / chapter_name / page（partial_update 追加）
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(KnowledgeConstant.METADATA_CHAPTER_ID, chapter.getId().intValue());
        metadata.put(KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
        if (StringUtils.hasText(request.getPageRef())) {
            metadata.put(KnowledgeConstant.METADATA_PAGE, request.getPageRef());
        }
        qaAgentClient.updateDocumentMetadata(requireDatasetId(), documentId, metadata);

        // 3. 落库 knowledge_content，保存 dify_document_id（事务内，失败自动回滚数据库）
        insertKnowledgeContent(request.getChapterId(), request.getContent(),
                request.getContentType(), request.getPageRef(), documentId);

        log.info("文本文档上传完成: documentId={}, chapterId={}", documentId, request.getChapterId());
        return new UploadResponse(documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResponse uploadFile(Long chapterId, MultipartFile file) {
        KnowledgeChapter chapter = requireChapter(chapterId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.UPLOAD_FILE_EMPTY);
        }
        log.info("上传文件文档: chapterId={}, fileName={}", chapterId, file.getOriginalFilename());

        // 原始文件名可能含路径或 Windows 非法字符（\ / : * ?），清洗后再作临时文件后缀
        String safeName = ObjectUtils.getDisplayString(file.getOriginalFilename())
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        File tempFile = null;
        try {
            tempFile = File.createTempFile("dify_upload_", "_" + safeName);
            file.transferTo(tempFile);
            Map<String, Object> metadata = Map.of(
                    KnowledgeConstant.METADATA_CHAPTER_ID, chapterId.intValue(),
                    KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
            //TODO 需增加请求失败时的分支处理
                       String documentId = qaAgentClient.createDocumentByFile(requireDatasetId(), tempFile,
                    file.getOriginalFilename(), metadata).getData();

            insertKnowledgeContent(chapterId, null, null, null, documentId);
            log.info("文件文档上传完成: documentId={}", documentId);
            return new UploadResponse(documentId);
        } catch (IOException e) {
            throw new BusinessException("文件暂存失败: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
            }
        }
    }

    @Override
    public String updateMetadata(UpdateMetadataRequest request) {
        log.info("更新文档元数据: documentId={}, metadata={}", request.getDocumentId(), request.getMetadata());
        //TODO 需增加请求失败时的分支处理
        return qaAgentClient.updateDocumentMetadata(requireDatasetId(),
                request.getDocumentId(), request.getMetadata()).getData();
    }

    @Override
    public VerifyResponse verifyContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorMessageConstant.VERIFY_CONTENT_EMPTY);
        }
        List<String> keywordList = loadKeywords();
        List<String> matched = new ArrayList<>();
        for (String keyword : keywordList) {
            if (content.contains(keyword)) {
                matched.add(keyword);
            }
        }
        boolean passed = !matched.isEmpty();
        log.info("AI 内容校验: passed={}, matched={}", passed, matched);
        return new VerifyResponse(passed, matched,
                passed ? ErrorMessageConstant.VERIFY_PASSED : ErrorMessageConstant.VERIFY_NOT_PASSED);
    }

    // ===== 对外接口实现（knowledge-api）=====

    @Override
    public Result<com.amlve.knowledgeapi.dto.response.KnowledgeResponse> queryKnowledge(String keywords) {
        KnowledgeResponse response = query(keywords);
        com.amlve.knowledgeapi.dto.response.KnowledgeResponse result =
                new com.amlve.knowledgeapi.dto.response.KnowledgeResponse();
        result.setQuery(response.getQuery());
        result.setMessage(response.getMessage());
        result.setItems(knowledgeConverter.toKnowledgeItems(response.getItems()));
        return Result.success(result);
    }

    /**
     * 根据章节ID获取该章节所有知识点权重配置（供出题智能体跨模块调用）
     * <p>
     * 委托给 {@link KnowledgeWeightConfigServiceImpl#listByChapter(Long)}，
     * 关联知识点名称的组装逻辑集中在权重配置服务内。
     */
    @Override
    public Result<List<WeightConfigVO>> getWeightConfigsByChapter(Long chapterId) {
        return Result.success(weightConfigService.listByChapter(chapterId));
    }

    // ===== 私有方法 =====

    /**
     * 调用 Dify 检索并转换为前端响应；技术异常映射为业务可读文案
     */
    private KnowledgeResponse doQuery(String keywords) {
        try {
            //TODO 需增加请求失败时的分支处理
            DifyRetrieveResponse retrieveResponse =
                    qaAgentClient.retrieve(requireDatasetId(), keywords, null, null).getData();
            KnowledgeResponse response = new KnowledgeResponse();
            response.setQuery(keywords);
            List<KnowledgeResponse.KnowledgeItem> items = new ArrayList<>();
            if (retrieveResponse.getRecords() != null) {
                for (DifyRetrieveResponse.Record record : retrieveResponse.getRecords()) {
                    KnowledgeResponse.KnowledgeItem item = new KnowledgeResponse.KnowledgeItem();
                    if (record.getSegment() != null) {
                        item.setSegmentId(record.getSegment().getId());
                        item.setContent(record.getSegment().getContent());
                        item.setDocumentId(record.getSegment().getDocument() == null
                                ? null : record.getSegment().getDocument().getId());
                        item.setDocumentName(record.getSegment().getDocument() == null
                                ? null : record.getSegment().getDocument().getName());
                    }
                    item.setScore(record.getScore());
                    // 章节与页码以 MySQL 为准：按 dify_document_id 反查 knowledge_content
                    // （Dify 把元数据放在 segment.document.doc_metadata，api DTO 未映射该层级）
                    if (item.getDocumentId() != null) {
                        KnowledgeContent content = contentMapper.selectOne(
                                Wrappers.lambdaQuery(KnowledgeContent.class)
                                        .eq(KnowledgeContent::getDifyDocumentId, item.getDocumentId())
                                        .last("LIMIT 1"));
                        if (content != null) {
                            item.setChapterId(content.getChapterId());
                            item.setPage(content.getPageRef());
                        }
                    }
                    items.add(item);
                }
            }
            response.setItems(items);
            return response;
        } catch (BusinessException e) {
            log.error("Dify 检索失败: {}", e.getMessage());
            throw new BusinessException(ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);
        }
    }

    private void insertKnowledgeContent(Long chapterId, String content, Integer contentType,
                                        String pageRef, String documentId) {
        KnowledgeContent knowledgeContent = new KnowledgeContent();
        knowledgeContent.setChapterId(chapterId);
        knowledgeContent.setContent(content);
        knowledgeContent.setContentType(contentType);
        knowledgeContent.setPageRef(pageRef);
        knowledgeContent.setDifyDocumentId(documentId);
        contentMapper.insert(knowledgeContent);
    }

    private KnowledgeChapter requireChapter(Long chapterId) {
        if (ObjectUtils.isEmpty(chapterId)) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_ID_EMPTY);
        }
        KnowledgeChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_NOT_FOUND);
        }
        return chapter;
    }

    /**
     * 写操作（上传/改元数据）仅限教师：JWT 令牌无角色信息，通过 system-api 编程式校验
     */
    private void requireTeacher() {
        Long userId = SecurityUtils.getUserId();
        if (!userService.isTeacher(userId)) {
            log.warn("权限不足: userId={} 尝试执行知识库写操作", userId);
            throw new BusinessException(ErrorMessageConstant.PERMISSION_DENIED);
        }
    }

    private String requireDatasetId() {
        String datasetId = configuration.getDifyDatasetId();
        if (!StringUtils.hasText(datasetId)) {
            throw new BusinessException(ErrorMessageConstant.DATASET_ID_MISSING);
        }
        return datasetId;
    }

    /**
     * 加载校验关键词：优先取 knowledge_keyword 表，表为空时使用兜底关键词
     */
    private List<String> loadKeywords() {
        List<KnowledgeKeyword> keywordList = keywordMapper.selectList(null);
        if (keywordList.isEmpty()) {
            log.info("knowledge_keyword 表无数据，使用兜底关键词");
            return Arrays.asList(KnowledgeConstant.FALLBACK_KEYWORDS.split(","));
        }
        List<String> result = new ArrayList<>();
        for (KnowledgeKeyword keyword : keywordList) {
            result.add(keyword.getKeyword());
            if (StringUtils.hasText(keyword.getSynonyms())) {
                result.addAll(Arrays.asList(keyword.getSynonyms().split(",")));
            }
        }
        return result;
    }
}
