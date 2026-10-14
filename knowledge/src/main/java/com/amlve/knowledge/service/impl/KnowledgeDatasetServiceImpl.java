package com.amlve.knowledge.service.impl;

import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.common.constant.ErrorCodeConstant;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
public abstract class KnowledgeDatasetServiceImpl implements KnowledgeDatasetService,com.amlve.knowledgeapi.service.KnowledgeService {

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

        // 1. Dify 创建文本文档（返回 documentId；createChapterDocument 冗余写元数据流程，故不用）
        String documentId = unwrap(qaAgentClient.createDocumentByText(
                requireDatasetId(), request.getDocumentName(), request.getContent()),
                ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

        // 2. 写入元数据：chapter_id / chapter_name / page（partial_update 追加）
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(KnowledgeConstant.METADATA_CHAPTER_ID, chapter.getId().intValue());
        metadata.put(KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
        if (StringUtils.hasText(request.getPageRef())) {
            metadata.put(KnowledgeConstant.METADATA_PAGE, request.getPageRef());
        }
        unwrap(qaAgentClient.updateDocumentMetadata(requireDatasetId(), documentId, metadata),
                ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

        // 3. 落库 knowledge_content，保存 dify_document_id（事务内，失败自动回滚数据库）
        saveKnowledgeContent(request.getChapterId(), request.getContent(),
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
        requireSupportedFileType(file.getOriginalFilename());
        log.info("上传文件文档: chapterId={}, fileName={}", chapterId, file.getOriginalFilename());

        // 原始文件名可能含路径或 Windows 非法字符（\ / : * ?），清洗后作为暂存文件名
        String safeName = ObjectUtils.getDisplayString(file.getOriginalFilename())
                .replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safeName.isEmpty()) {
            safeName = "document";
        }
        // 必须放进独立子目录并保留原始文件名：Dify 的 create-by-file 以"上传文件名"作知识库文档名
        // （data 里的 name 字段会被忽略），若用 File.createTempFile 加随机前缀，
        // 文档名就会变成 dify_upload_123456_讲义.txt 这种带随机数的脏名字，且每次上传都不重名
        Path tempDir = null;
        File tempFile = null;
        try {
            tempDir = Files.createTempDirectory("dify_upload_");
            tempFile = tempDir.resolve(safeName).toFile();
            file.transferTo(tempFile);
            Map<String, Object> metadata = Map.of(
                    KnowledgeConstant.METADATA_CHAPTER_ID, chapterId.intValue(),
                    KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
            String documentId = unwrap(qaAgentClient.createDocumentByFile(requireDatasetId(), tempFile,
                    safeName, metadata), ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

            saveKnowledgeContent(chapterId, null, null, null, documentId);
            log.info("文件文档上传完成: documentId={}, documentName={}", documentId, safeName);
            return new UploadResponse(documentId);
        } catch (IOException e) {
            throw new BusinessException("文件暂存失败: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempFile);
            deleteQuietly(tempDir == null ? null : tempDir.toFile());
        }
    }

    /**
     * 删除暂存文件/目录，失败只告警不打断主流程
     */
    private void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.warn("临时文件删除失败: {}", file.getAbsolutePath());
        }
    }

    @Override
    public String updateMetadata(UpdateMetadataRequest request) {
        log.info("更新文档元数据: documentId={}, metadata={}", request.getDocumentId(), request.getMetadata());
        return unwrap(qaAgentClient.updateDocumentMetadata(requireDatasetId(),
                request.getDocumentId(), request.getMetadata()), ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);
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
            DifyRetrieveResponse retrieveResponse = unwrap(
                    qaAgentClient.retrieve(requireDatasetId(), keywords, null, null),
                    ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);
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

    /**
     * 落库文档记录：按 dify_document_id 做 upsert。
     * <p>
     * Dify 建档对同名文档是幂等的——重复上传返回的是同一个 documentId。
     * 若这里无条件 insert，同一文档会在 knowledge_content 堆出多条记录，
     * 而检索时是按 documentId 反查章节与页码（LIMIT 1），可能命中过期记录，
     * 删除文档时也会留下孤儿行。
     * <p>
     * 字段覆盖规则：章节归属以本次上传为准（教师把文档挪到别的章节即生效）；
     * 正文、类型、页码仅在本次传入非 null 时覆盖——文件上传通道拿不到文本，
     * 不能把此前文本通道写入的正文抹成空。
     */
    private void saveKnowledgeContent(Long chapterId, String content, Integer contentType,
                                      String pageRef, String documentId) {
        KnowledgeContent exist = contentMapper.selectOne(Wrappers.lambdaQuery(KnowledgeContent.class)
                .eq(KnowledgeContent::getDifyDocumentId, documentId)
                .orderByDesc(KnowledgeContent::getId)
                .last("LIMIT 1"));
        if (exist == null) {
            KnowledgeContent knowledgeContent = new KnowledgeContent();
            knowledgeContent.setChapterId(chapterId);
            knowledgeContent.setContent(content);
            knowledgeContent.setContentType(contentType);
            knowledgeContent.setPageRef(pageRef);
            knowledgeContent.setDifyDocumentId(documentId);
            contentMapper.insert(knowledgeContent);
            return;
        }
        exist.setChapterId(chapterId);
        if (content != null) {
            exist.setContent(content);
        }
        if (contentType != null) {
            exist.setContentType(contentType);
        }
        if (pageRef != null) {
            exist.setPageRef(pageRef);
        }
        contentMapper.updateById(exist);
        log.info("同名文档已存在，更新原记录: contentId={}, documentId={}", exist.getId(), documentId);
    }

    /**
     * 解包跨模块调用的 Result：下游返回失败码时转为业务异常。
     * <p>
     * 直接 {@code .getData()} 会在失败时拿到 null 并继续往下走，
     * 表现为"接口成功但数据为空"的假成功（对应测试报告 S-07 的失败分支缺口）。
     *
     * @param result    下游返回的结果包装
     * @param failLabel 失败时对外展示的文案
     * @return 结果数据（可能为 null，如写操作无返回值）
     */
    private <T> T unwrap(Result<T> result, String failLabel) {
        if (result == null) {
            log.error("跨模块调用无响应: {}", failLabel);
            throw new BusinessException(failLabel);
        }
        if (result.getCode() != ErrorCodeConstant.SUCCESS) {
            log.error("跨模块调用失败: code={}, message={}", result.getCode(), result.getMessage());
            throw new BusinessException(failLabel);
        }
        return result.getData();
    }

    /**
     * 校验上传文件扩展名是否在 Dify 支持的文档类型白名单内。
     * <p>
     * zip 等压缩包、无扩展名文件会被 Dify 建档接口直接 400 拒绝，
     * 在入口处提前拦截并返回明确文案（对应测试报告 F-01）。
     */
    private void requireSupportedFileType(String originalFilename) {
        String filename = ObjectUtils.getDisplayString(originalFilename);
        int dotIndex = filename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? filename.substring(dotIndex + 1).toLowerCase() : "";
        if (!KnowledgeConstant.UPLOAD_FILE_ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("文件类型不受支持: fileName={}", originalFilename);
            throw new BusinessException(ErrorMessageConstant.UPLOAD_FILE_TYPE_UNSUPPORTED);
        }
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
