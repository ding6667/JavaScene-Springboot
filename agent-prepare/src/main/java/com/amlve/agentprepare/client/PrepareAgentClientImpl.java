package com.amlve.agentprepare.client;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.agentprepare.bean.DifyClient;
import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.HomeworkConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.LessonPlanConfig;
import com.amlve.agentprepareapi.dto.request.PrepareRequest.PptConfig;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 备课智能体客户端实现。
 * <p>
 * 职责：把 teach 模块传入的开关、七类素材、三个生成配置与风格令牌组装为 Dify 工作流入参
 * （settings + reference + user_message + styleTokens + 三个配置），blocking 调用备课工作流
 * 并解析返回的教案/PPT大纲/重难点/习题/实训五字段。
 * 未配置工作流密钥时走 Mock 兜底，便于本地无 Dify 环境跑通链路。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PrepareAgentClientImpl implements PrepareAgentClient {

    /** 忽略 Dify 响应中的未知字段，仅解析业务所需字段 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DifyClient difyClient;

    /**
     * 备课工作流应用密钥（app- 开头）：优先读 dify.prepare-api-key，
     * 未配置时回落 dify.api-key；两者均空则走 Mock 兜底
     */
    @Value("${dify.prepare-api-key}")
    private String prepareApiKey;

    /** 工作流开始节点新增入参键（snake_case，与工作流 v2 契约一致，键名常量收敛） */
    private static final String KEY_PPT_CONFIG = "ppt_config";
    private static final String KEY_LESSON_PLAN_CONFIG = "lesson_plan_config";
    private static final String KEY_HOMEWORK_CONFIG = "homework_config";
    private static final String KEY_STYLE_TOKENS = "styleTokens";

    @Override
    public Result<PrepareResponse> generate(PrepareRequest request, TeachingMaterialDTO teachingMaterial) {
        // 密钥未配置：Mock 兜底，返回模板材料
        if (prepareApiKey == null || prepareApiKey.isBlank()) {
            log.warn("未配置 dify.prepare-api-key，备课智能体走 Mock 兜底返回模板材料");
            return Result.success(mockGenerate(request));
        }

        log.info("调用备课智能体: chapterId={}, pptx={}, lessonPlan={}, homework={}, lab={}, keyPoints={}, userMessage={}",
                request.getChapterId(), request.getPptx(), request.getLessonPlan(),
                request.getHomework(), request.getLab(), request.getKeyPoints(), request.getUserMessage());

        // 1. 组装工作流入参
        Map<String, Object> inputs = buildInputs(request, teachingMaterial);

        // 2. blocking 调用 Dify 备课工作流
        String response = difyClient.runWorkflowBlocking(prepareApiKey, inputs);

        // 3. 解析返回的五字段并组装响应
        PrepareResponse result = parseResponse(response, request);
        log.info("备课智能体返回: chapterId={}, lessonPlanLen={}, pptxMdLen={}, keyPointsLen={}, homeworkLen={}, labLen={}",
                request.getChapterId(),
                len(result.getLessonPlan()), len(result.getPptx()), len(result.getKeyPoints()),
                len(result.getHomework()), len(result.getLab()));
        return Result.success(result);
    }

    /**
     * 组装 Dify 工作流 inputs：settings（五个开关）+ reference（七类素材）+ user_message（用户额外指令，必填）
     */
    private Map<String, Object> buildInputs(PrepareRequest request, TeachingMaterialDTO material) {
        // settings：五个生成开关，字段名为工作流开始节点定义的 snake_case
        Map<String, Object> settings = new HashMap<>();
        settings.put("pptx", request.getPptx());
        settings.put("lesson_plan", request.getLessonPlan());
        settings.put("homework", request.getHomework());
        settings.put("lab", request.getLab());
        settings.put("key_points", request.getKeyPoints());

        // reference：七类素材（knowledge 模块已按 page/pageOrder 升序序列化为 JSON 字符串），此处仅透传；
        // 素材为 null 时各键传空字符串（工作流按固定键提取，缺失键可能导致其内部提取节点异常）
        TeachingMaterialDTO m = material != null ? material : TeachingMaterialDTO.builder().build();
        Map<String, Object> reference = new HashMap<>();
        reference.put("chapter_summary", nullToEmpty(m.getChapterSummary()));
        reference.put("concept_definitions", nullToEmpty(m.getConceptDefinitions()));
        reference.put("code_examples", nullToEmpty(m.getCodeExamples()));
        reference.put("common_mistakes", nullToEmpty(m.getCommonMistakes()));
        reference.put("page_references", nullToEmpty(m.getPageReferences()));
        reference.put("exercises", nullToEmpty(m.getExercises()));
        reference.put("lab_tasks", nullToEmpty(m.getLabTasks()));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("settings", settings);
        inputs.put("reference", reference);
        // 工作流开始节点要求 user_message 必填，为空传空字符串
        inputs.put("user_message", nullToEmpty(request.getUserMessage()));
        // 风格令牌：teach 侧已序列化为 JSON 字符串，工作流做白名单过滤；空串表示默认风格
        inputs.put(KEY_STYLE_TOKENS, nullToEmpty(request.getStyleTokens()));
        // 三个生成配置（工作流 v2 新增）：始终传完整对象，config 为 null 时传空键值对，
        // 由工作流代码节点兜底默认值；null 字段键统一空字符串，避免提取节点缺键异常
        inputs.put(KEY_PPT_CONFIG, buildPptConfigMap(request.getPptConfig()));
        inputs.put(KEY_LESSON_PLAN_CONFIG, buildLessonPlanConfigMap(request.getLessonPlanConfig()));
        inputs.put(KEY_HOMEWORK_CONFIG, buildHomeworkConfigMap(request.getHomeworkConfig()));
        return inputs;
    }

    /** PPT 配置 → 工作流 ppt_config（键名与工作流代码节点读取键一致） */
    private Map<String, Object> buildPptConfigMap(PptConfig config) {
        Map<String, Object> map = new HashMap<>();
        if (config != null) {
            map.put("structure", nullToEmpty(config.getStructure()));
            map.put("pageRange", nullToEmpty(config.getPageRange()));
            map.put("includeOutline", nullToEmpty(config.getIncludeOutline()));
            map.put("includeSummary", nullToEmpty(config.getIncludeSummary()));
            map.put("codeBlockMax", nullToEmpty(config.getCodeBlockMax()));
        }
        return map;
    }

    /** 教案配置 → 工作流 lesson_plan_config */
    private Map<String, Object> buildLessonPlanConfigMap(LessonPlanConfig config) {
        Map<String, Object> map = new HashMap<>();
        if (config != null) {
            map.put("courseType", nullToEmpty(config.getCourseType()));
            map.put("duration", nullToEmpty(config.getDuration()));
            map.put("detailLevel", nullToEmpty(config.getDetailLevel()));
            map.put("includeBoardDesign", nullToEmpty(config.getIncludeBoardDesign()));
            map.put("includeExercise", nullToEmpty(config.getIncludeExercise()));
        }
        return map;
    }

    /** 课后习题配置 → 工作流 homework_config */
    private Map<String, Object> buildHomeworkConfigMap(HomeworkConfig config) {
        Map<String, Object> map = new HashMap<>();
        if (config != null) {
            map.put("questionCount", nullToEmpty(config.getQuestionCount()));
            map.put("difficultyDistribution", nullToEmpty(config.getDifficultyDistribution()));
            map.put("questionBank", nullToEmpty(config.getQuestionBank()));
        }
        return map;
    }

    /**
     * 解析工作流 blocking 响应：校验执行状态，提取五字段（空串归一化为 null）。
     * <p>
     * 响应结构：{ data: { status, outputs: { output: {lesson_plan, pptx_md, key_points, homework, lab} }, error } }。
     * 结束节点输出变量名为 output（套 LLM structured_output），兼容回落 outputs 直取。
     */
    private PrepareResponse parseResponse(String response, PrepareRequest request) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode dataNode = root.path("data");
            String status = dataNode.path("status").asText("");
            if (!"succeeded".equals(status)) {
                String error = dataNode.path("error").asText("未知错误");
                log.error("Dify 备课工作流未执行成功: status={}, error={}", status, error);
                throw new BusinessException(ErrorMessageConstant.AGENT_UNAVAILABLE);
            }
            JsonNode outputs = dataNode.path("outputs");
            if (outputs.isMissingNode() || outputs.isNull()) {
                log.error("Dify 备课工作流响应缺少 outputs 节点");
                throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE);
            }
            // 优先取结束节点变量 output，未命中时回落 outputs 直取（兼容工作流后续调整）
            JsonNode outputNode = outputs.path("output");
            JsonNode source = outputNode.isObject() ? outputNode : outputs;

            PrepareResponse result = new PrepareResponse();
            result.setLessonPlan(normalize(source.path("lesson_plan").asText(null)));
            result.setPptx(normalize(source.path("pptx_md").asText(null)));
            result.setKeyPoints(normalize(source.path("key_points").asText(null)));
            result.setHomework(normalize(source.path("homework").asText(null)));
            result.setLab(normalize(source.path("lab").asText(null)));
            // 开关关闭的字段不透传（防止模型违规生成）
            if (!isOn(request.getLessonPlan())) {
                result.setLessonPlan(null);
            }
            if (!isOn(request.getPptx())) {
                result.setPptx(null);
            }
            if (!isOn(request.getKeyPoints())) {
                result.setKeyPoints(null);
            }
            if (!isOn(request.getHomework())) {
                result.setHomework(null);
            }
            if (!isOn(request.getLab())) {
                result.setLab(null);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorMessageConstant.AGENT_RESULT_UNREADABLE, e);
        }
    }

    /**
     * Mock 兜底：按请求开关返回模板备课材料（Marp 格式 PPT 大纲），供无 Dify 环境联调
     */
    private PrepareResponse mockGenerate(PrepareRequest request) {
        String chapter = "第" + request.getChapterId() + "章";
        PrepareResponse response = new PrepareResponse();
        if (isOn(request.getLessonPlan())) {
            response.setLessonPlan("【教案模板 - " + chapter + "】\n"
                    + "一、教学目标：掌握本章核心知识点，能独立完成课后例题。\n"
                    + "二、教学重难点：本章重点概念辨析与常见易错点。\n"
                    + "三、课堂例题：结合教材例题讲解，演示完整解题步骤。\n"
                    + "四、上机实训任务：安排教材配套实操案例，学生独立完成并提交。\n"
                    + "五、课后作业：布置教材课后习题，巩固本章知识点。");
        }
        if (isOn(request.getKeyPoints())) {
            response.setKeyPoints("【重难点清单 - " + chapter + "】\n"
                    + "1. 重点：本章核心概念及其应用场景。\n"
                    + "2. 难点：易混淆概念辨析与综合运用。");
        }
        if (isOn(request.getHomework())) {
            response.setHomework("【课后习题 - " + chapter + "】\n"
                    + "1. 简答题：概述本章核心概念及适用场景。\n"
                    + "2. 编程题：完成教材本章课后编程练习。");
        }
        if (isOn(request.getLab())) {
            response.setLab("【上机实训方案 - " + chapter + "】\n"
                    + "任务：基于本章知识点完成综合实操案例，并提交运行结果。");
        }
        if (isOn(request.getPptx())) {
            // Mock 返回 Marp 格式 PPT 大纲（新契约 pptx 为 Markdown 源码，文件生成由 teach 负责）
            response.setPptx("# " + chapter + "\n\n教学要点概览\n\n---\n\n# 本章内容\n\n1. 核心概念\n2. 典型示例\n3. 课后练习\n\n---\n\n# 总结\n\n巩固本章知识点");
        }
        return response;
    }

    /** 判断生成开关是否开启（"1"=开启，其余视为关闭） */
    private boolean isOn(String flag) {
        return "1".equals(flag);
    }

    /** null 转空字符串（工作流必填参数不能为 null） */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Dify 对关闭的字段返回空字符串，归一化为 null，避免下游误当有效内容 */
    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private int len(String value) {
        return value == null ? 0 : value.length();
    }
}
