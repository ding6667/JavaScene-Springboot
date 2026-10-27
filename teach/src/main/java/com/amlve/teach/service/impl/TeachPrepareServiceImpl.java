package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.dto.request.PrepareRequest.HomeworkConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.LessonPlanConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.PptConfig;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.enums.PrepareRecordStatusEnum;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.TeachPrepareAsyncService;
import com.amlve.teach.service.TeachPrepareService;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 备课智能体业务实现（同步受理部分）。
 * <p>
 * 编排流程：校验 PPT 模板 → 查素材（knowledge-api）→ 落一条 status=2 备课记录并取得记录ID →
 * 把智能体调用与 PPT 转换交给 {@link TeachPrepareAsyncService} 后台执行 → 秒回记录ID 供前端轮询。
 * <p>
 * 模板与素材校验留在受理阶段：入参问题仍以业务异常直接返回，不会留下半成品记录。
 * 本方法不加事务，insert 立即提交，后台线程才能按ID回写同一条记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachPrepareServiceImpl implements TeachPrepareService {

    private final KnowledgeChapterApi knowledgeChapterApi;

    private final TeachPrepareRecordMapper teachPrepareRecordMapper;

    private final TeachPptTemplateMapper teachPptTemplateMapper;

    private final TeachPrepareAsyncService teachPrepareAsyncService;

    @Override
    public PrepareResponse generateTeachingMaterials(PrepareRequest request) {
        Long teacherId = SecurityUtils.getUserId();

        // 1. 解析 PPT 模板：templateId 为空用系统默认模板，非空校验可用性（系统或本人且启用）
        TeachPptTemplate template = resolveTemplate(request.getTemplateId(), teacherId);
        String themeCode = template != null ? template.getThemeCode() : null;

        // 2. 跨模块查询章节备课素材（knowledge 已按 page/pageOrder 升序序列化），失败即中断备课
        Result<com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO> materialResult =
                knowledgeChapterApi.getTeachingMaterials(request.getChapterId());
        if (materialResult == null || materialResult.getCode() != ErrorCodeConstant.SUCCESS) {
            throw new BusinessException("查询备课素材失败: "
                    + (materialResult == null ? "接口返回为空" : materialResult.getMessage()));
        }
        com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO material = materialResult.getData();

        // 3. 受理落库：主键为雪花ID（IdType.ASSIGN_ID），insert 后即可返回给前端
        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setChapterId(request.getChapterId());
        record.setTeacherId(teacherId);
        record.setStatus(PrepareRecordStatusEnum.GENERATING.getCode());
        teachPrepareRecordMapper.insert(record);

        // 4. 智能体调用与 PPT 转换投入后台线程，材料生成完成后按 recordId 回写同一条记录
        teachPrepareAsyncService.generateAsync(record.getId(), request.getChapterId(), teacherId,
                toApiRequest(request, template), toApiMaterial(material), themeCode);

        PrepareResponse response = new PrepareResponse();
        response.setChapterId(request.getChapterId());
        response.setRecordId(record.getId());
        response.setStatus(record.getStatus());
        response.setStatusDesc(PrepareRecordStatusEnum.nameOf(record.getStatus()));
        response.setGenerateTime(record.getCreateTime());
        log.info("备课已受理: chapterId={}, teacherId={}, recordId={}",
                request.getChapterId(), teacherId, record.getId());
        return response;
    }

    /**
     * 解析 PPT 模板：templateId 为空取系统默认模板（sort_order 最小）；
     * 非空校验存在、启用且归属可用（系统模板或本人自定义），否则业务异常。
     * 系统模板均未初始化时返回 null 降级（不注入主题、不下发骨架，走旧链路行为）。
     */
    private TeachPptTemplate resolveTemplate(Long templateId, Long teacherId) {
        if (templateId == null) {
            List<TeachPptTemplate> systemTemplates = teachPptTemplateMapper.selectList(
                    new LambdaQueryWrapper<TeachPptTemplate>()
                            .eq(TeachPptTemplate::getIsSystem, 1)
                            .eq(TeachPptTemplate::getIsActive, 1)
                            .orderByAsc(TeachPptTemplate::getSortOrder));
            TeachPptTemplate defaultTemplate = systemTemplates.isEmpty() ? null : systemTemplates.get(0);
            if (defaultTemplate == null) {
                log.warn("未找到可用的系统 PPT 模板（初始化 SQL 未执行？），本次备课降级为无模板模式");
            }
            return defaultTemplate;
        }

        TeachPptTemplate template = teachPptTemplateMapper.selectById(templateId);
        boolean system = template != null && template.getIsSystem() != null && template.getIsSystem() == 1;
        boolean own = template != null && teacherId.equals(template.getTeacherId());
        boolean active = template != null && template.getIsActive() != null && template.getIsActive() == 1;
        if (!active || (!system && !own)) {
            throw new BusinessException("PPT模板不存在或不可用");
        }
        return template;
    }

    /** 前端请求 → 智能体 api 请求（字段对齐 api 契约，模板骨架填充 ppt_config.structure） */
    private com.amlve.agentprepareapi.dto.request.PrepareRequest toApiRequest(PrepareRequest request,
                                                                              TeachPptTemplate template) {
        com.amlve.agentprepareapi.dto.request.PrepareRequest apiRequest =
                new com.amlve.agentprepareapi.dto.request.PrepareRequest();
        apiRequest.setChapterId(request.getChapterId());
        apiRequest.setLessonPlan(request.getLessonPlan());
        apiRequest.setPptx(request.getPptx());
        apiRequest.setHomework(request.getHomework());
        apiRequest.setLab(request.getLab());
        apiRequest.setKeyPoints(request.getKeyPoints());
        apiRequest.setUserMessage(request.getUserMessage());
        apiRequest.setTemplateId(request.getTemplateId() == null ? null : String.valueOf(request.getTemplateId()));

        // PPT 配置：模板骨架 structureMd 优先填充 structure（无模板时空串，由工作流兜底）
        PptConfig pptConfig = new PptConfig();
        if (request.getPptConfig() != null) {
            pptConfig.setPageRange(request.getPptConfig().getPageRange());
            pptConfig.setIncludeOutline(request.getPptConfig().getIncludeOutline());
            pptConfig.setIncludeSummary(request.getPptConfig().getIncludeSummary());
            pptConfig.setCodeBlockMax(request.getPptConfig().getCodeBlockMax());
        }
        pptConfig.setStructure(template != null && StringUtils.hasText(template.getStructureMd())
                ? template.getStructureMd() : "");
        apiRequest.setPptConfig(pptConfig);

        // 教案配置：直接透传
        if (request.getLessonPlanConfig() != null) {
            LessonPlanConfig config = new LessonPlanConfig();
            config.setCourseType(request.getLessonPlanConfig().getCourseType());
            config.setDuration(request.getLessonPlanConfig().getDuration());
            config.setDetailLevel(request.getLessonPlanConfig().getDetailLevel());
            config.setIncludeBoardDesign(request.getLessonPlanConfig().getIncludeBoardDesign());
            config.setIncludeExercise(request.getLessonPlanConfig().getIncludeExercise());
            apiRequest.setLessonPlanConfig(config);
        }

        // 习题配置：questionBank 待 exam-api 题库接口就绪后填充（getQuestionsForPrepare），当前空串降级
        if (request.getHomeworkConfig() != null) {
            HomeworkConfig config = new HomeworkConfig();
            config.setQuestionCount(request.getHomeworkConfig().getQuestionCount());
            config.setDifficultyDistribution(request.getHomeworkConfig().getDifficultyDistribution());
            config.setQuestionBank("");
            apiRequest.setHomeworkConfig(config);
        }
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
