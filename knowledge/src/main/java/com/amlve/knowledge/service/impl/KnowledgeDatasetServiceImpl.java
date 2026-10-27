package com.amlve.knowledge.service.impl;

import com.amlve.agentqaapi.service.QAAgentClient;
import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledge.Configuration;
import com.amlve.knowledge.constant.ErrorMessageConstant;
import com.amlve.knowledge.constant.KnowledgeConstant;
import com.amlve.knowledge.converter.KnowledgeConverter;
import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import com.amlve.knowledge.mapper.KnowledgeChapterMapper;
import com.amlve.knowledge.mapper.KnowledgeContentMapper;
import com.amlve.knowledge.mapper.KnowledgePointMapper;
import com.amlve.knowledge.service.KnowledgeDatasetService;
import com.amlve.knowledge.service.KnowledgeUploadRecordService;
import com.amlve.knowledge.service.KnowledgeWeightConfigService;
import com.amlve.knowledge.enumeration.UploadTypeEnum;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import com.amlve.knowledge.tableEnt.KnowledgeContent;
import com.amlve.knowledge.tableEnt.KnowledgeUploadRecord;
import com.amlve.knowledgeapi.dto.response.WeightConfigVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * 知识库数据集服务实现
 * <p>
 * 同时实现 {@link com.amlve.knowledgeapi.service.KnowledgeService}，
 * 供其他模块通过 knowledge-api 接口调用（跨模块禁止直接注入本实现类）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDatasetServiceImpl implements KnowledgeDatasetService,com.amlve.knowledgeapi.service.KnowledgeService {

    private final Configuration configuration;
    private final QAAgentClient qaAgentClient;
    private final KnowledgeChapterMapper chapterMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final KnowledgeConverter knowledgeConverter;
    private final KnowledgeWeightConfigService weightConfigService;
    private final KnowledgeUploadRecordService uploadRecordService;
    /**
     * 上传任务线程池（{@code KnowledgeAsyncConfig#UPLOAD_EXECUTOR}）。
     * 字段名与 Bean 名一致：容器里若再出现第二个 TaskExecutor，Spring 按参数名匹配到这个。
     */
    private final TaskExecutor knowledgeUploadExecutor;

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

    /**
     * 文本文档上传（异步受理，对应任务书 3.11"长耗时占线程"）。
     * <p>
     * 请求线程内只做三件事：校验章节/知识库配置、建立"处理中"留痕、把建档任务投入后台线程，
     * 然后立刻返回 recordId。Dify 建档后的索引轮询最长 5 分钟，放在请求线程里会同时占住
     * Tomcat 工作线程和数据库连接（HikariCP 默认 10 连接，约 10 个并发上传就能把池打满）。
     * <p>
     * 刻意不加 @Transactional：本方法只有一次 insert（留痕），单语句天然原子；后台流程同理，
     * 且远程等待绝不能被圈进事务。
     */
    @Override
    public UploadResponse uploadText(UploadTextRequest request) {
        KnowledgeChapter chapter = requireChapter(request.getChapterId());
        String datasetId = requireDatasetId();

        // 留痕放在可校验参数就绪之后：start 自身失败也不能顶掉上传主流程
        KnowledgeUploadRecord record = uploadRecordService.start(datasetId, chapter.getId(), chapter.getTitle(),
                request.getDocumentName(),
                (long) request.getContent().getBytes(StandardCharsets.UTF_8).length,
                textFileType(request.getDocumentName()), UploadTypeEnum.CREATE.getCode());
        log.info("文本文档上传已受理: recordId={}, chapterId={}, documentName={}",
                record.getId(), request.getChapterId(), request.getDocumentName());

        submitUpload(record, () -> completeTextUpload(datasetId, chapter, request, record));
        return UploadResponse.accepted(record.getId());
    }

    /**
     * 后台完成文本文档建档：建档 → 写元数据 → 落库 → 回写留痕终态。
     * <p>
     * 失败不再向外抛异常（没有调用方接），一律以留痕记录的 status=2 + errorMsg 呈现，
     * 前端从 {@code /upload/list} 读失败原因。
     */
    private void completeTextUpload(String datasetId, KnowledgeChapter chapter,
                                    UploadTextRequest request, KnowledgeUploadRecord record) {
        DifyDocumentVO document = null;
        try {
            // 1. Dify 创建文本文档（内部已轮询索引并做空壳/失败清理）
            document = unwrap(qaAgentClient.createDocumentByText(
                    datasetId, request.getDocumentName(), request.getContent()),
                    ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

            // 2. 写入元数据：chapter_id / chapter_name / page（partial_update 追加）
            Map<String, Object> metadata = new HashMap<>();
            // 章节ID按 Long 原样传：主键已改雪花算法，intValue() 会静默溢出成乱值
            metadata.put(KnowledgeConstant.METADATA_CHAPTER_ID, chapter.getId());
            metadata.put(KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
            if (request.getPage() != null) {
                // Dify 侧 page 元数据字段已建为字符串类型，这里按字符串传值避免类型不匹配
                metadata.put(KnowledgeConstant.METADATA_PAGE, String.valueOf(request.getPage()));
            }
            unwrap(qaAgentClient.updateDocumentMetadata(datasetId, document.getDocumentId(), metadata),
                    ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

            // 3. 落库 knowledge_content，保存 dify_document_id
            boolean existed = saveKnowledgeContent(chapter.getId(), request.getContent(),
                    request.getContentType(), request.getPage(), document.getDocumentId());

            uploadRecordService.markSuccess(record, document,
                    existed ? UploadTypeEnum.UPDATE.getCode() : UploadTypeEnum.CREATE.getCode());
            log.info("文本文档上传完成: recordId={}, documentId={}, chapterId={}, totalSegments={}",
                    record.getId(), document.getDocumentId(), request.getChapterId(), document.getTotalSegments());
        } catch (Exception e) {
            cleanupDifyDocument(datasetId, document, e);
            uploadRecordService.markFailed(record,
                    document == null ? null : document.getDocumentId(),
                    document == null ? null : document.getBatch(), e.getMessage());
            log.error("文本文档上传失败: recordId={}, chapterId={}, documentName={}",
                    record.getId(), request.getChapterId(), request.getDocumentName(), e);
        }
    }

    /**
     * 文件文档上传（异步受理，口径同 {@link #uploadText}）。
     * <p>
     * 暂存落盘必须留在请求线程：{@code MultipartFile} 的底层临时文件随请求结束被容器删除，
     * 异步线程再去读只会拿到"文件不存在"。
     */
    @Override
    public UploadResponse uploadFile(Long chapterId, MultipartFile file) {
        KnowledgeChapter chapter = requireChapter(chapterId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorMessageConstant.UPLOAD_FILE_EMPTY);
        }
        requireSupportedFileType(file.getOriginalFilename());
        String datasetId = requireDatasetId();

        // 原始文件名可能含路径或 Windows 非法字符（\ / : * ?），清洗后作为暂存文件名；
        // getOriginalFilename() 可能为 null，ObjectUtils 会给出字符串 "null"，必须显式兜底
        String rawName = file.getOriginalFilename();
        String cleanedName = StringUtils.hasText(rawName)
                ? rawName.replaceAll("[\\\\/:*?\"<>|]", "_").trim() : "";
        String safeName = cleanedName.isEmpty() ? "document.txt" : cleanedName;
        KnowledgeUploadRecord record = uploadRecordService.start(datasetId, chapter.getId(), chapter.getTitle(),
                safeName, file.getSize(), extensionOf(safeName), UploadTypeEnum.CREATE.getCode());

        // 必须放进独立子目录并保留原始文件名：Dify 的 create-by-file 以"上传文件名"作知识库文档名
        // （data 里的 name 字段会被忽略），若用 File.createTempFile 加随机前缀，
        // 文档名就会变成 dify_upload_123456_讲义.txt 这种带随机数的脏名字，且每次上传都不重名
        Path tempDir = null;
        File tempFile = null;
        try {
            tempDir = Files.createTempDirectory("dify_upload_");
            tempFile = tempDir.resolve(safeName).toFile();
            file.transferTo(tempFile);
        } catch (IOException e) {
            deleteQuietly(tempFile);
            deleteQuietly(tempDir == null ? null : tempDir.toFile());
            uploadRecordService.markFailed(record, null, null, "文件暂存失败: " + e.getMessage());
            log.warn("FILE_TEMP_STORE_FAILED 定位: {}", e.getMessage());
            throw new BusinessException(ErrorMessageConstant.FILE_TEMP_STORE_FAILED, e);
        }

        // lambda 只能捕获 effectively final 变量，把已落盘的路径固定下来交给后台线程
        File stagedFile = tempFile;
        Path stagedDir = tempDir;
        boolean submitted = false;
        try {
            submitUpload(record, () -> completeFileUpload(datasetId, chapter, stagedFile, stagedDir, safeName, record));
            submitted = true;
        } finally {
            // 未入队（队列满被拒）时暂存文件没人删，请求线程直接清理，否则临时目录会越攒越多
            if (!submitted) {
                deleteQuietly(stagedFile);
                deleteQuietly(stagedDir.toFile());
            }
        }
        log.info("文件文档上传已受理: recordId={}, chapterId={}, fileName={}",
                record.getId(), chapterId, safeName);
        return UploadResponse.accepted(record.getId());
    }

    /**
     * 后台完成文件建档，结束时清理暂存文件
     */
    private void completeFileUpload(String datasetId, KnowledgeChapter chapter, File tempFile, Path tempDir,
                                    String safeName, KnowledgeUploadRecord record) {
        DifyDocumentVO document = null;
        try {
            Map<String, Object> metadata = Map.of(
                    KnowledgeConstant.METADATA_CHAPTER_ID, chapter.getId(),
                    KnowledgeConstant.METADATA_CHAPTER_NAME, chapter.getTitle());
            document = unwrap(qaAgentClient.createDocumentByFile(datasetId, tempFile,
                    safeName, metadata), ErrorMessageConstant.KNOWLEDGE_SERVICE_UNAVAILABLE);

            boolean existed = saveKnowledgeContent(chapter.getId(), null, null, null, document.getDocumentId());
            uploadRecordService.markSuccess(record, document,
                    existed ? UploadTypeEnum.UPDATE.getCode() : UploadTypeEnum.CREATE.getCode());
            log.info("文件文档上传完成: recordId={}, documentId={}, documentName={}, totalSegments={}",
                    record.getId(), document.getDocumentId(), safeName, document.getTotalSegments());
        } catch (Exception e) {
            cleanupDifyDocument(datasetId, document, e);
            uploadRecordService.markFailed(record,
                    document == null ? null : document.getDocumentId(),
                    document == null ? null : document.getBatch(), e.getMessage());
            log.error("文件文档上传失败: recordId={}, fileName={}", record.getId(), safeName, e);
        } finally {
            deleteQuietly(tempFile);
            deleteQuietly(tempDir.toFile());
        }
    }

    /**
     * 投递后台建档任务。
     * <p>
     * 队列被拒时把留痕直接写成失败并回业务异常：记录必须有终态，否则
     * {@code /upload/list} 里永远挂着一条"处理中"，前端只能干等。
     */
    private void submitUpload(KnowledgeUploadRecord record, Runnable task) {
        try {
            knowledgeUploadExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            log.warn("上传任务队列已满，拒绝受理: recordId={}", record.getId());
            uploadRecordService.markFailed(record, null, null, ErrorMessageConstant.UPLOAD_QUEUE_FULL);
            throw new BusinessException(ErrorMessageConstant.UPLOAD_QUEUE_FULL);
        }
    }

    /**
     * 建档成功但后续步骤（写元数据 / 落库）失败时，删除 Dify 侧文档。
     * <p>
     * 不删就会留下一个已建索引、但没有本地映射的"活孤儿文档"，它会持续污染检索结果。
     * 索引阶段自身的失败已由 agent-qa 内部清理，这里只兜后续步骤。
     */
    private void cleanupDifyDocument(String datasetId, DifyDocumentVO document, Exception cause) {
        if (document == null || !StringUtils.hasText(document.getDocumentId())) {
            return;
        }
        try {
            qaAgentClient.deleteDocument(datasetId, document.getDocumentId());
            log.info("已清理建档后失败的残留文档: documentId={}", document.getDocumentId());
        } catch (Exception e) {
            log.error("残留文档清理失败，需人工核对知识库: documentId={}, 原始失败={}",
                    document.getDocumentId(), cause.getMessage(), e);
        }
    }

    /**
     * 文本通道的"文件类型"：文档名通常没有扩展名，此时按内容形态记 txt，
     * 避免该列恒为 NULL 变成没有信息量的死列
     */
    private String textFileType(String documentName) {
        String extension = extensionOf(documentName);
        return extension != null ? extension : "txt";
    }

    /**
     * 取文件名扩展名（小写，不含点）；无扩展名返回 null
     */
    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 && dotIndex < fileName.length() - 1
                ? fileName.substring(dotIndex + 1).toLowerCase() : null;
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
                            item.setPage(content.getPage());
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
     *
     * @return true 表示命中已有记录（本次为更新），false 表示新增
     */
    private boolean saveKnowledgeContent(Long chapterId, String content, Integer contentType,
                                         Integer page, String documentId) {
        KnowledgeContent exist = contentMapper.selectOne(Wrappers.lambdaQuery(KnowledgeContent.class)
                .eq(KnowledgeContent::getDifyDocumentId, documentId)
                .orderByDesc(KnowledgeContent::getId)
                .last("LIMIT 1"));
        if (exist == null) {
            KnowledgeContent knowledgeContent = new KnowledgeContent();
            knowledgeContent.setChapterId(chapterId);
            // 表结构要求 content 非空；文件通道拿不到正文（原文只存在 Dify 侧），
            // 这里落空串作为"文档→章节"的映射占位，不伪造教材内容
            knowledgeContent.setContent(content != null ? content : "");
            knowledgeContent.setContentType(contentType);
            knowledgeContent.setPage(page);
            knowledgeContent.setDifyDocumentId(documentId);
            contentMapper.insert(knowledgeContent);
            return false;
        }
        exist.setChapterId(chapterId);
        if (content != null) {
            exist.setContent(content);
        }
        if (contentType != null) {
            exist.setContentType(contentType);
        }
        if (page != null) {
            exist.setPage(page);
        }
        contentMapper.updateById(exist);
        log.info("同名文档已存在，更新原记录: contentId={}, documentId={}", exist.getId(), documentId);
        return true;
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

    private String requireDatasetId() {
        String datasetId = configuration.getDifyDatasetId();
        if (!StringUtils.hasText(datasetId)) {
            throw new BusinessException(ErrorMessageConstant.DATASET_ID_MISSING);
        }
        return datasetId;
    }
}