package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.service.TeachPrepareService;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.util.MarkdownToPptxUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 备课智能体业务实现。
 * <p>
 * 编排流程：查素材（knowledge-api）→ 调备课智能体（agent-prepare-api）→ PPT Markdown 转 pptx 文件 →
 * 备课记录/PPT 记录落库 → 组装前端响应。
 * Dify 远程调用不置于事务内；失败时先落 status=0 备课记录再抛出业务异常。
 */
@Slf4j
@Service
public class TeachPrepareServiceImpl implements TeachPrepareService {

    /** 错误信息落库截断长度 */
    private static final int ERROR_MSG_MAX = 500;

    @Resource
    private KnowledgeChapterApi knowledgeChapterApi;

    @Resource
    private PrepareAgentClient prepareAgentClient;

    @Resource
    private MarkdownToPptxUtil markdownToPptxUtil;

    @Resource
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Resource
    private TeachPptMapper teachPptMapper;

    @Override
    public PrepareResponse generateTeachingMaterials(PrepareRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        try {
            // 1. 跨模块查询章节备课素材（knowledge 已按 page/pageOrder 升序序列化）
            // TODO 需处理result.code == error 异常情况
            com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO material =
                    knowledgeChapterApi.getTeachingMaterials(request.getChapterId()).getData();

            // 2. 调用备课智能体（素材 DTO 跨模块转换后传入）
            // TODO 需处理 Result 异常情况
            com.amlve.agentprepareapi.dto.response.PrepareResponse agentResult =
                    prepareAgentClient.generate(toApiRequest(request), toApiMaterial(material)).getData();

            // 3. PPT Markdown 转 pptx 文件（转换失败返回 null，其余材料正常返回）
            String pptxUrl = null;
            Long pptxFileSize = null;
            String pptxFileName = null;
            if (agentResult.getPptx() != null && !agentResult.getPptx().isBlank()) {
                pptxUrl = markdownToPptxUtil.convertContent(agentResult.getPptx());
                if (pptxUrl != null) {
                    pptxFileName = Paths.get(pptxUrl).getFileName().toString();
                    try {
                        pptxFileSize = Files.size(Paths.get(pptxUrl));
                    } catch (IOException e) {
                        log.warn("读取 PPT 文件大小失败: {}", pptxUrl, e);
                    }
                } else {
                    log.warn("PPT 转换失败（marp2pptx 不可用？），本次仅返回其余备课材料: chapterId={}", request.getChapterId());
                }
            }

            // 4. 备课记录落库（status=1 成功）
            TeachPrepareRecord record = new TeachPrepareRecord();
            record.setChapterId(request.getChapterId());
            record.setTeacherId(teacherId);
            record.setLessonPlan(agentResult.getLessonPlan());
            record.setKeyPoints(agentResult.getKeyPoints());
            record.setHomework(agentResult.getHomework());
            record.setLab(agentResult.getLab());
            record.setPptxMd(agentResult.getPptx());
            record.setPptxUrl(pptxUrl);
            record.setStatus(1);
            teachPrepareRecordMapper.insert(record);

            // 5. PPT 文件记录落库（仅生成了文件才落）
            if (pptxUrl != null) {
                TeachPpt ppt = new TeachPpt();
                ppt.setChapterId(request.getChapterId());
                ppt.setFileName(pptxFileName);
                ppt.setFilePath(pptxUrl);
                ppt.setMdContent(agentResult.getPptx());
                ppt.setFileSize(pptxFileSize);
                ppt.setDownloadCount(0);
                teachPptMapper.insert(ppt);
            }

            // 6. 组装前端响应
            PrepareResponse response = new PrepareResponse();
            response.setChapterId(request.getChapterId());
            response.setLessonPlan(agentResult.getLessonPlan());
            response.setPptxUrl(pptxUrl);
            response.setKeyPoints(agentResult.getKeyPoints());
            response.setHomework(agentResult.getHomework());
            response.setLab(agentResult.getLab());
            response.setRecordId(record.getId());
            response.setGenerateTime(record.getCreateTime());
            log.info("备课生成成功: chapterId={}, teacherId={}, recordId={}, pptxUrl={}",
                    request.getChapterId(), teacherId, record.getId(), pptxUrl);
            return response;
        } catch (BusinessException e) {
            // 7. 失败兜底：落 status=0 备课记录后原样上抛
            log.warn("备课生成失败: chapterId={}, teacherId={}, error={}",
                    request.getChapterId(), teacherId, e.getMessage());
            saveFailedRecord(request.getChapterId(), teacherId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 非预期异常（如 IO）：同样落失败记录
            log.error("备课生成异常: chapterId={}", request.getChapterId(), e);
            saveFailedRecord(request.getChapterId(), teacherId, e.getMessage());
            throw new BusinessException("备课生成失败：" + e.getMessage(), e);
        }
    }

    /** 落库失败备课记录（status=0，错误信息截断） */
    private void saveFailedRecord(Long chapterId, Long teacherId, String errorMsg) {
        try {
            TeachPrepareRecord record = new TeachPrepareRecord();
            record.setChapterId(chapterId);
            record.setTeacherId(teacherId);
            record.setStatus(0);
            record.setErrorMsg(errorMsg == null ? null
                    : errorMsg.substring(0, Math.min(errorMsg.length(), ERROR_MSG_MAX)));
            teachPrepareRecordMapper.insert(record);
        } catch (Exception ex) {
            // 落库失败只记日志，不影响原异常上抛
            log.error("保存失败备课记录异常: chapterId={}", chapterId, ex);
        }
    }

    /** 前端请求 → 智能体 api 请求（字段对齐 api 契约） */
    private com.amlve.agentprepareapi.dto.request.PrepareRequest toApiRequest(PrepareRequest request) {
        com.amlve.agentprepareapi.dto.request.PrepareRequest apiRequest =
                new com.amlve.agentprepareapi.dto.request.PrepareRequest();
        apiRequest.setChapterId(request.getChapterId());
        apiRequest.setLessonPlan(request.getLessonPlan());
        apiRequest.setPptx(request.getPptx());
        apiRequest.setHomework(request.getHomework());
        apiRequest.setLab(request.getLab());
        apiRequest.setKeyPoints(request.getKeyPoints());
        apiRequest.setUserMessage(request.getUserMessage());
        return apiRequest;
    }

    /** knowledge-api 素材 DTO → agent-prepare-api 素材 DTO（逐字段拷贝，避免跨模块类加载问题） */
    private TeachingMaterialDTO toApiMaterial(com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO source) {
        if (source == null) {
            return TeachingMaterialDTO.builder().build();
        }
        return TeachingMaterialDTO.builder()
                .chapterSummary(source.getChapterSummary())
                .conceptDefinitions(source.getConceptDefinitions())
                .codeExamples(source.getCodeExamples())
                .commonMistakes(source.getCommonMistakes())
                .pageReferences(source.getPageReferences())
                .exercises(source.getExercises())
                .labTasks(source.getLabTasks())
                .build();
    }
}
