package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.HomeworkConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.LessonPlanConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.PptConfig;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.constant.MarpThemeConstants;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.service.TeachPrepareService;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.util.MarkdownToPptxUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

    /** 风格令牌序列化器（令牌为简单键值对，不依赖容器内定制配置） */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** frontmatter 分隔线（独占一行的 ---），用于定位已含 frontmatter 内容的闭合位置 */
    private static final java.util.regex.Pattern FRONTMATTER_DELIM =
            java.util.regex.Pattern.compile("(?m)^---\\s*$");

    /** 风格令牌颜色值校验：仅允许十六进制或 rgb(a)，杜绝样式注入 */
    private static final java.util.regex.Pattern CSS_COLOR_PATTERN =
            java.util.regex.Pattern.compile("^(#[0-9a-fA-F]{3,8}|rgba?\\([0-9.,%\\s]{3,40}\\))$");

    /** 风格令牌字体值校验：字体名、引号、逗号与空白 */
    private static final java.util.regex.Pattern CSS_FONT_PATTERN =
            java.util.regex.Pattern.compile("^[\\w\\u4e00-\\u9fa5 ,'\"-]{1,120}$");

    /** 风格令牌字重值校验 */
    private static final java.util.regex.Pattern CSS_WEIGHT_PATTERN =
            java.util.regex.Pattern.compile("^(normal|bold|bolder|lighter|[1-9]00)$");

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

    @Resource
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @Override
    public PrepareResponse generateTeachingMaterials(PrepareRequest request) {
        Long teacherId = SecurityUtils.getUserId();
        try {
            // 1. 解析 PPT 模板：templateId 为空用系统默认模板，非空校验可用性（系统或本人且启用）
            TeachPptTemplate template = resolveTemplate(request.getTemplateId(), teacherId);
            String themeCode = template != null ? template.getThemeCode() : null;
            // 风格令牌：请求显式传入优先，其次模板 design_tokens，最后回退主题预置令牌
            java.util.Map<String, Object> styleTokens = resolveStyleTokens(request.getStyleTokens(), template);

            // 2. 跨模块查询章节备课素材（knowledge 已按 page/pageOrder 升序序列化），失败即中断备课
            Result<com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO> materialResult =
                    knowledgeChapterApi.getTeachingMaterials(request.getChapterId());
            if (materialResult == null || materialResult.getCode() != ErrorCodeConstant.SUCCESS) {
                throw new BusinessException("查询备课素材失败: "
                        + (materialResult == null ? "接口返回为空" : materialResult.getMessage()));
            }
            com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO material = materialResult.getData();

            // 3. 调用备课智能体（素材 DTO 跨模块转换后传入，模板骨架随 ppt_config 下发），失败即中断备课
            Result<com.amlve.agentprepareapi.dto.response.PrepareResponse> agentResultRaw =
                    prepareAgentClient.generate(toApiRequest(request, template, styleTokens), toApiMaterial(material));
            if (agentResultRaw == null || agentResultRaw.getCode() != ErrorCodeConstant.SUCCESS) {
                throw new BusinessException("备课智能体调用失败: "
                        + (agentResultRaw == null ? "接口返回为空" : agentResultRaw.getMessage()));
            }
            com.amlve.agentprepareapi.dto.response.PrepareResponse agentResult = agentResultRaw.getData();

            // 4. PPT Markdown 注入视觉主题 frontmatter 后转 pptx 文件（转换失败返回 null，其余材料正常返回）
            String pptxUrl = null;
            Long pptxFileSize = null;
            String pptxFileName = null;
            if (agentResult.getPptx() != null && !agentResult.getPptx().isBlank()) {
                pptxUrl = markdownToPptxUtil.convertContent(
                        injectTheme(agentResult.getPptx(), themeCode, styleTokens));
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

            // 5. 备课记录落库（status=1 成功）
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

            // 6. PPT 文件记录落库（仅生成了文件才落；md_content 存 Dify 返回的原始内容，不含主题 frontmatter）
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

            // 7. 组装前端响应
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

    /**
     * 解析 PPT 风格令牌：请求显式传入（非空）优先，其次是模板库内 design_tokens，
     * 最后回退 themeCode 对应的预置令牌。
     */
    private java.util.Map<String, Object> resolveStyleTokens(java.util.Map<String, Object> requestTokens,
                                                             TeachPptTemplate template) {
        if (requestTokens != null && !requestTokens.isEmpty()) {
            return requestTokens;
        }
        if (template == null) {
            return null;
        }
        java.util.Map<String, Object> stored = template.getDesignTokens();
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return MarpThemeConstants.getDesignTokens(template.getThemeCode());
    }

    /**
     * PPT Markdown 注入 Marp 基础 frontmatter、主题样式与风格令牌覆写。
     * <p>
     * marp2pptx 不支持向 Marp CLI 透传 --theme-set，自定义 CSS 主题以 {@code <style>} 块
     * 内联进 Markdown（Marp 会将其应用到每页并覆盖默认外观）。主题 CSS 之后追加由风格令牌
     * 生成的白名单覆写规则（颜色/字体/字重），实现模板主题的个性化微调。
     * <ul>
     *     <li>内容无 frontmatter：头部拼接 frontmatter + style 块；</li>
     *     <li>内容已含 frontmatter（如 Dify 输出自带）：style 块插入到 frontmatter 闭合线之后，不重复注入基础配置；</li>
     *     <li>既无主题样式也无可覆写令牌：原样返回。</li>
     * </ul>
     */
    private String injectTheme(String contentMd, String themeCode, java.util.Map<String, Object> styleTokens) {
        String themeCss = MarpThemeConstants.getThemeStylesheet(themeCode);
        String overrideCss = buildTokenOverrideCss(styleTokens);
        if (themeCss == null && overrideCss.isEmpty()) {
            return contentMd;
        }
        StringBuilder css = new StringBuilder();
        if (themeCss != null) {
            css.append(themeCss);
        }
        if (!overrideCss.isEmpty()) {
            if (css.length() > 0) {
                css.append('\n');
            }
            css.append("/* design tokens */\n").append(overrideCss);
        }
        String styleBlock = "<style>\n" + css + "\n</style>\n\n";
        if (!contentMd.startsWith("---")) {
            // 无 frontmatter：注入基础配置 + 主题样式
            return "---\nmarp: true\npaginate: true\nsize: 16:9\n---\n\n" + styleBlock + contentMd;
        }
        // 已含 frontmatter：定位第 2 条 --- 分隔线（闭合线），样式插在其后；
        // 找不到闭合线时放弃注入，避免破坏原文结构
        java.util.regex.Matcher matcher = FRONTMATTER_DELIM.matcher(contentMd);
        int closeEnd = -1;
        for (int i = 0; i < 2 && matcher.find(); i++) {
            closeEnd = matcher.end();
        }
        if (closeEnd > 0) {
            return contentMd.substring(0, closeEnd) + "\n\n" + styleBlock + contentMd.substring(closeEnd);
        }
        return contentMd;
    }

    /**
     * 由风格令牌生成覆写 CSS：取值逐项通过白名单校验，非法字段静默忽略（不阻断备课）。
     */
    private String buildTokenOverrideCss(java.util.Map<String, Object> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }
        String primaryColor = safeToken(tokens.get("primaryColor"), CSS_COLOR_PATTERN);
        String backgroundColor = safeToken(tokens.get("backgroundColor"), CSS_COLOR_PATTERN);
        String textColor = safeToken(tokens.get("textColor"), CSS_COLOR_PATTERN);
        String codeBgColor = safeToken(tokens.get("codeBgColor"), CSS_COLOR_PATTERN);
        String fontFamily = safeToken(tokens.get("fontFamily"), CSS_FONT_PATTERN);
        String headingWeight = safeToken(tokens.get("headingWeight"), CSS_WEIGHT_PATTERN);

        StringBuilder css = new StringBuilder();
        appendRule(css, "section",
                "background", backgroundColor,
                "color", textColor,
                "font-family", fontFamily);
        appendRule(css, "h1, h2, h3",
                "color", primaryColor,
                "font-weight", headingWeight);
        appendRule(css, "pre, code",
                "background", codeBgColor);
        return css.toString();
    }

    /** 追加一条 CSS 规则；声明值均为空时不输出空规则 */
    private void appendRule(StringBuilder css, String selector, String... declarations) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i + 1 < declarations.length; i += 2) {
            String value = declarations[i + 1];
            if (value != null) {
                body.append("  ").append(declarations[i]).append(": ").append(value).append(";\n");
            }
        }
        if (body.length() > 0) {
            css.append(selector).append(" {\n").append(body).append("}\n");
        }
    }

    /** 字符串令牌白名单校验：非字符串或格式不符返回 null */
    private String safeToken(Object value, java.util.regex.Pattern pattern) {
        if (!(value instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        return pattern.matcher(trimmed).matches() ? trimmed : null;
    }

    /** 前端请求 → 智能体 api 请求（字段对齐 api 契约，模板骨架填充 ppt_config.structure） */
    private com.amlve.agentprepareapi.dto.request.PrepareRequest toApiRequest(PrepareRequest request,
                                                                              TeachPptTemplate template,
                                                                              java.util.Map<String, Object> styleTokens) {
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
        // 风格令牌：JSON 字符串透传工作流；空令牌传空串，工作流按默认风格处理
        apiRequest.setStyleTokens(toStyleTokensJson(styleTokens));

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

    /** 风格令牌 → JSON 字符串；空令牌返回空串，序列化异常降级空串（不阻断备课） */
    private String toStyleTokensJson(java.util.Map<String, Object> styleTokens) {
        if (styleTokens == null || styleTokens.isEmpty()) {
            return "";
        }
        try {
            return JSON_MAPPER.writeValueAsString(styleTokens);
        } catch (Exception e) {
            log.warn("风格令牌序列化失败，本次按默认风格处理", e);
            return "";
        }
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
