package com.amlve.agentprepare.client;

import com.amlve.agentprepare.bean.DifyClient;
import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 备课智能体客户端单元测试：Mock DifyClient，验证入参组装与响应解析。
 */
@ExtendWith(MockitoExtension.class)
class PrepareAgentClientImplTest {

    @Mock
    private DifyClient difyClient;

    @InjectMocks
    private PrepareAgentClientImpl client;

    private static final String API_KEY = "app-test-key";

    /** 配置密钥并构造默认全开请求 */
    private PrepareRequest buildRequest() {
        ReflectionTestUtils.setField(client, "prepareApiKey", API_KEY);
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(5L);
        return request;
    }

    /** 构造素材 DTO（字段值即 knowledge 模块产出的 JSON 字符串） */
    private TeachingMaterialDTO buildMaterial() {
        return TeachingMaterialDTO.builder()
                .chapterSummary("本章介绍类与对象")
                .conceptDefinitions("[{\"name\":\"类\",\"definition\":\"...\"}]")
                .codeExamples("[{\"title\":\"定义类\",\"code\":\"...\"}]")
                .commonMistakes("[{\"description\":\"忘记写构造方法\"}]")
                .pageReferences("{\"类的定义\":\"P.110-115\"}")
                .exercises("[{\"question\":\"...\"}]")
                .labTasks("[]")
                .build();
    }

    /** Dify blocking 成功响应（结束节点变量 output 包裹五字段） */
    private String successResponse(String lessonPlan, String pptxMd) {
        return "{\"data\":{\"status\":\"succeeded\",\"outputs\":{\"output\":{"
                + "\"lesson_plan\":\"" + lessonPlan + "\","
                + "\"pptx_md\":\"" + pptxMd + "\","
                + "\"key_points\":\"## 教学重点\","
                + "\"homework\":\"## 选择题\","
                + "\"lab\":\"## 实训目标\"}}}}";
    }

    @Test
    void generate_shouldBuildThreeInputKeys_whenDifyConfigured() {
        PrepareRequest request = buildRequest();
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        client.generate(request, buildMaterial());

        // 验证 inputs 含 settings/reference/user_message 三个必填键
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyClient).runWorkflowBlocking(anyString(), captor.capture());
        Map<String, Object> inputs = captor.getValue();
        assertThat(inputs).containsKeys("settings", "reference", "user_message");

        Map<String, Object> settings = (Map<String, Object>) inputs.get("settings");
        assertThat(settings.get("pptx")).isEqualTo("1");
        assertThat(settings.get("lesson_plan")).isEqualTo("1");

        Map<String, Object> reference = (Map<String, Object>) inputs.get("reference");
        assertThat(reference.get("chapter_summary")).isEqualTo("本章介绍类与对象");
        assertThat(reference.get("concept_definitions")).isEqualTo("[{\"name\":\"类\",\"definition\":\"...\"}]");
        assertThat(reference.get("lab_tasks")).isEqualTo("[]");
    }

    @Test
    void generate_shouldPassEmptyUserMessage_whenUserMessageNull() {
        PrepareRequest request = buildRequest();
        request.setUserMessage(null);
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        client.generate(request, buildMaterial());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyClient).runWorkflowBlocking(anyString(), captor.capture());
        // 工作流 user_message 必填，null 应转为空字符串
        assertThat(captor.getValue().get("user_message")).isEqualTo("");
    }

    @Test
    void generate_shouldParseFiveFields_fromNestedOutput() {
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("## 教案内容", "# PPT --- # 第二页"));

        PrepareResponse response = client.generate(buildRequest(), buildMaterial()).getData();

        assertThat(response.getLessonPlan()).isEqualTo("## 教案内容");
        assertThat(response.getPptx()).contains("第二页");
        assertThat(response.getKeyPoints()).isEqualTo("## 教学重点");
        assertThat(response.getHomework()).isEqualTo("## 选择题");
        assertThat(response.getLab()).isEqualTo("## 实训目标");
    }

    @Test
    void generate_shouldFallbackToFlatOutputs_whenNoOutputWrapper() {
        // 兼容工作流调整：outputs 直取五字段（无 output 包裹层）
        String flatResponse = "{\"data\":{\"status\":\"succeeded\",\"outputs\":{"
                + "\"lesson_plan\":\"plan\",\"pptx_md\":\"ppt\",\"key_points\":\"kp\","
                + "\"homework\":\"hw\",\"lab\":\"lab\"}}}";
        when(difyClient.runWorkflowBlocking(anyString(), anyMap())).thenReturn(flatResponse);

        PrepareResponse response = client.generate(buildRequest(), buildMaterial()).getData();

        assertThat(response.getLessonPlan()).isEqualTo("plan");
        assertThat(response.getPptx()).isEqualTo("ppt");
    }

    @Test
    void generate_shouldThrow_whenWorkflowFailed() {
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn("{\"data\":{\"status\":\"failed\",\"error\":\"LLM 超时\"}}");

        assertThatThrownBy(() -> client.generate(buildRequest(), buildMaterial()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI 服务暂时不可用");
    }

    @Test
    void generate_shouldNormalizeBlankToNull_whenSwitchOff() {
        // Dify 对关闭字段返回空字符串，且开关关闭时即使模型违规生成也应置空
        PrepareRequest request = buildRequest();
        request.setHomework("0");
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        PrepareResponse response = client.generate(request, buildMaterial()).getData();

        assertThat(response.getHomework()).isNull();
        assertThat(response.getLessonPlan()).isEqualTo("plan");
    }

    @Test
    void generate_shouldReturnMock_whenApiKeyBlank() {
        // 密钥未配置：走 Mock 兜底，不发起 Dify 调用
        ReflectionTestUtils.setField(client, "prepareApiKey", "");
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(3L);

        PrepareResponse response = client.generate(request, buildMaterial()).getData();

        assertThat(response.getLessonPlan()).contains("第3章");
        assertThat(response.getPptx()).contains("---");
        verify(difyClient, org.mockito.Mockito.never()).runWorkflowBlocking(anyString(), anyMap());
    }

    @Test
    void generate_shouldHandleNullMaterial() {
        // 素材为 null（如调用方未传）：reference 各键为空字符串，不阻断
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        PrepareResponse response = client.generate(buildRequest(), null).getData();

        assertThat(response.getLessonPlan()).isEqualTo("plan");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyClient).runWorkflowBlocking(anyString(), captor.capture());
        Map<String, Object> reference = (Map<String, Object>) captor.getValue().get("reference");
        assertThat(reference.get("chapter_summary")).isEqualTo("");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_shouldBuildThreeConfigMaps_whenConfigsProvided() {
        // 三 config 齐传：组装为 ppt_config / lesson_plan_config / homework_config，键与工作流读取键一致
        PrepareRequest request = buildRequest();
        PrepareRequest.PptConfig pptConfig = new PrepareRequest.PptConfig();
        pptConfig.setStructure("# 骨架");
        pptConfig.setPageRange("8-12");
        pptConfig.setIncludeOutline("1");
        pptConfig.setIncludeSummary("0");
        pptConfig.setCodeBlockMax("2");
        request.setPptConfig(pptConfig);
        PrepareRequest.LessonPlanConfig lessonPlanConfig = new PrepareRequest.LessonPlanConfig();
        lessonPlanConfig.setCourseType("new");
        lessonPlanConfig.setDuration("90");
        lessonPlanConfig.setDetailLevel("detailed");
        request.setLessonPlanConfig(lessonPlanConfig);
        PrepareRequest.HomeworkConfig homeworkConfig = new PrepareRequest.HomeworkConfig();
        homeworkConfig.setQuestionCount("5");
        homeworkConfig.setDifficultyDistribution("2,2,1");
        homeworkConfig.setQuestionBank("[]");
        request.setHomeworkConfig(homeworkConfig);
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        client.generate(request, buildMaterial());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyClient).runWorkflowBlocking(anyString(), captor.capture());
        Map<String, Object> inputs = captor.getValue();
        assertThat(inputs).containsKeys("ppt_config", "lesson_plan_config", "homework_config");

        Map<String, Object> pptConfigMap = (Map<String, Object>) inputs.get("ppt_config");
        assertThat(pptConfigMap.get("structure")).isEqualTo("# 骨架");
        assertThat(pptConfigMap.get("pageRange")).isEqualTo("8-12");
        assertThat(pptConfigMap.get("includeOutline")).isEqualTo("1");
        assertThat(pptConfigMap.get("includeSummary")).isEqualTo("0");
        assertThat(pptConfigMap.get("codeBlockMax")).isEqualTo("2");

        Map<String, Object> lessonPlanConfigMap = (Map<String, Object>) inputs.get("lesson_plan_config");
        assertThat(lessonPlanConfigMap.get("courseType")).isEqualTo("new");
        assertThat(lessonPlanConfigMap.get("duration")).isEqualTo("90");
        assertThat(lessonPlanConfigMap.get("detailLevel")).isEqualTo("detailed");

        Map<String, Object> homeworkConfigMap = (Map<String, Object>) inputs.get("homework_config");
        assertThat(homeworkConfigMap.get("questionCount")).isEqualTo("5");
        assertThat(homeworkConfigMap.get("difficultyDistribution")).isEqualTo("2,2,1");
        assertThat(homeworkConfigMap.get("questionBank")).isEqualTo("[]");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generate_shouldPassEmptyConfigMaps_whenConfigsNull() {
        // 三 config 均为 null：键仍存在且为空 map，由工作流代码节点兜底默认值
        when(difyClient.runWorkflowBlocking(anyString(), anyMap()))
                .thenReturn(successResponse("plan", "ppt"));

        client.generate(buildRequest(), buildMaterial());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(difyClient).runWorkflowBlocking(anyString(), captor.capture());
        Map<String, Object> inputs = captor.getValue();
        assertThat((Map<String, Object>) inputs.get("ppt_config")).isEmpty();
        assertThat((Map<String, Object>) inputs.get("lesson_plan_config")).isEmpty();
        assertThat((Map<String, Object>) inputs.get("homework_config")).isEmpty();
    }
}
