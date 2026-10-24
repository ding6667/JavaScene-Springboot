package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.util.MarkdownToPptxUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 备课业务编排单元测试：Mock 素材接口/智能体客户端/PPT工具/Mapper，
 * 验证 DTO 转换、落库字段、失败兜底与 recordId 回填。
 */
@ExtendWith(MockitoExtension.class)
class TeachPrepareServiceImplTest {

    @Mock
    private KnowledgeChapterApi knowledgeChapterApi;

    @Mock
    private PrepareAgentClient prepareAgentClient;

    @Mock
    private MarkdownToPptxUtil markdownToPptxUtil;

    @Mock
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Mock
    private TeachPptMapper teachPptMapper;

    @Mock
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @InjectMocks
    private TeachPrepareServiceImpl service;

    private static final Long TEACHER_ID = 1L;
    private static final Long CHAPTER_ID = 5L;
    private static final String PPTX_PATH = "D:/temp/commonFile/2026/09/08/uuid.pptx";

    @BeforeEach
    void setUpLoginTeacher() {
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolderTestUtil.logout();
    }

    private PrepareRequest buildRequest() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setUserMessage("侧重案例讲解");
        return request;
    }

    /** knowledge-api 素材 DTO */
    private com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO buildKnowledgeMaterial() {
        return com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO.builder()
                .chapterSummary("本章介绍类与对象")
                .conceptDefinitions("[{\"name\":\"类\"}]")
                .codeExamples("[{\"title\":\"示例\"}]")
                .commonMistakes("[{\"description\":\"易错点\"}]")
                .pageReferences("{\"类\":\"P.110\"}")
                .exercises("[{\"question\":\"题目\"}]")
                .labTasks("[]")
                .build();
    }

    /** 智能体返回（含 PPT Markdown） */
    private com.amlve.agentprepareapi.dto.response.PrepareResponse buildAgentResult() {
        com.amlve.agentprepareapi.dto.response.PrepareResponse result =
                new com.amlve.agentprepareapi.dto.response.PrepareResponse();
        result.setLessonPlan("## 教案");
        result.setPptx("# PPT\n\n---\n\n# 第二页");
        result.setKeyPoints("## 重难点");
        result.setHomework("## 习题");
        result.setLab("## 实训");
        return result;
    }

    /** 模拟 MyBatis-Plus insert 主键回填 */
    private void mockInsertBackfillId() {
        doAnswer(invocation -> {
            TeachPrepareRecord r = invocation.getArgument(0);
            r.setId(100L);
            return 1;
        }).when(teachPrepareRecordMapper).insert(any(TeachPrepareRecord.class));
    }

    @Test
    void generate_shouldConvertMaterialFields_andSaveSuccessRecord() {
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class),
                any(com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO.class)))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        PrepareResponse response = service.generateTeachingMaterials(buildRequest());

        // 响应字段完整，recordId 回填
        assertThat(response.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(response.getLessonPlan()).isEqualTo("## 教案");
        assertThat(response.getPptxUrl()).isEqualTo(PPTX_PATH);
        assertThat(response.getKeyPoints()).isEqualTo("## 重难点");
        assertThat(response.getHomework()).isEqualTo("## 习题");
        assertThat(response.getLab()).isEqualTo("## 实训");
        assertThat(response.getRecordId()).isEqualTo(100L);

        // 备课记录：字段映射完整、teacherId 从登录上下文注入、status=1
        ArgumentCaptor<TeachPrepareRecord> recordCaptor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(recordCaptor.capture());
        TeachPrepareRecord record = recordCaptor.getValue();
        assertThat(record.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(record.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(record.getLessonPlan()).isEqualTo("## 教案");
        assertThat(record.getPptxMd()).contains("第二页");
        assertThat(record.getPptxUrl()).isEqualTo(PPTX_PATH);
        assertThat(record.getStatus()).isEqualTo(1);

        // PPT 记录：文件名/路径/Markdown 落库
        ArgumentCaptor<TeachPpt> pptCaptor = ArgumentCaptor.forClass(TeachPpt.class);
        verify(teachPptMapper).insert(pptCaptor.capture());
        TeachPpt ppt = pptCaptor.getValue();
        assertThat(ppt.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(ppt.getFileName()).isEqualTo("uuid.pptx");
        assertThat(ppt.getFilePath()).isEqualTo(PPTX_PATH);
        assertThat(ppt.getMdContent()).contains("第二页");
        assertThat(ppt.getDownloadCount()).isEqualTo(0);

        // 素材 DTO 逐字段拷贝验证
        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO> materialCaptor =
                ArgumentCaptor.forClass(com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO.class);
        verify(prepareAgentClient).generate(any(), materialCaptor.capture());
        com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO material = materialCaptor.getValue();
        assertThat(material.getChapterSummary()).isEqualTo("本章介绍类与对象");
        assertThat(material.getConceptDefinitions()).isEqualTo("[{\"name\":\"类\"}]");
        assertThat(material.getLabTasks()).isEqualTo("[]");
    }

    @Test
    void generate_shouldNotInsertTeachPpt_whenPptxNull() {
        // 智能体未返回 PPT（开关关闭）：不生成文件、不落 PPT 记录
        com.amlve.agentprepareapi.dto.response.PrepareResponse agentResult = buildAgentResult();
        agentResult.setPptx(null);
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(), any())).thenReturn(Result.success(agentResult));
        mockInsertBackfillId();

        PrepareResponse response = service.generateTeachingMaterials(buildRequest());

        assertThat(response.getPptxUrl()).isNull();
        verify(markdownToPptxUtil, never()).convertContent(anyString());
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
        // 其余材料正常返回，备课记录仍成功落库
        assertThat(response.getLessonPlan()).isEqualTo("## 教案");
        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void generate_shouldDegrade_whenPptxConvertFail() {
        // marp2pptx 不可用：pptxUrl=null，其余材料正常返回
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(), any())).thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(null);
        mockInsertBackfillId();

        PrepareResponse response = service.generateTeachingMaterials(buildRequest());

        assertThat(response.getPptxUrl()).isNull();
        assertThat(response.getLessonPlan()).isEqualTo("## 教案");
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
        // 备课记录仍成功（PPT 记录 url 为 null）
        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        assertThat(captor.getValue().getPptxUrl()).isNull();
    }

    @Test
    void generate_shouldSaveFailedRecord_andRethrow_whenMaterialQueryFail() {
        // 素材查询失败（章节不存在）：落 status=0 记录并上抛
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenThrow(new BusinessException("章节不存在"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("章节不存在");

        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        TeachPrepareRecord record = captor.getValue();
        assertThat(record.getStatus()).isEqualTo(0);
        assertThat(record.getErrorMsg()).contains("章节不存在");
        assertThat(record.getTeacherId()).isEqualTo(TEACHER_ID);
        // 失败路径不触达智能体与 PPT 记录
        verify(prepareAgentClient, never()).generate(any(), any());
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
    }

    @Test
    void generate_shouldSaveFailedRecord_andRethrow_whenAgentFail() {
        // 智能体调用失败：同样落 status=0 记录并上抛
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(), any()))
                .thenThrow(new BusinessException("Dify 备课工作流执行失败: status=failed"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dify");

        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(captor.getValue().getErrorMsg()).contains("Dify");
    }

    @Test
    void generate_shouldSaveFailedRecord_andRethrow_whenMaterialQueryReturnError() {
        // 素材接口返回 error Result（不抛异常）：判错后中断备课并落失败记录
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenReturn(Result.error("章节不存在"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查询备课素材失败");

        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(captor.getValue().getErrorMsg()).contains("查询备课素材失败");
        // 判错路径不触达智能体与 PPT 记录
        verify(prepareAgentClient, never()).generate(any(), any());
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
    }

    @Test
    void generate_shouldSaveFailedRecord_andRethrow_whenAgentReturnError() {
        // 智能体返回 error Result（不抛异常）：判错后中断备课并落失败记录
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(), any()))
                .thenReturn(Result.error("工作流执行失败"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("备课智能体调用失败");

        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        assertThat(captor.getValue().getErrorMsg()).contains("备课智能体调用失败");
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
    }

    /** 系统模板（templateId 为空时的默认模板） */
    private TeachPptTemplate systemTemplate() {
        TeachPptTemplate template = new TeachPptTemplate();
        template.setId(10L);
        template.setName("Java标准教学");
        template.setThemeCode("java-uncover");
        template.setStructureMd("# 第X章 骨架\n\n---\n\n# 总结");
        template.setIsSystem(1);
        template.setIsActive(1);
        template.setSortOrder(1);
        return template;
    }

    @Test
    void generate_shouldResolveDefaultTemplate_andInjectTheme_whenTemplateIdNull() {
        // templateId 未传：取 sort_order 最小的系统模板，骨架随 ppt_config 下发、主题注入转换入参
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(systemTemplate()));
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        service.generateTeachingMaterials(buildRequest());

        // 模板骨架结构下发智能体
        ArgumentCaptor<com.amlve.agentprepareapi.dto.request.PrepareRequest> apiCaptor =
                ArgumentCaptor.forClass(com.amlve.agentprepareapi.dto.request.PrepareRequest.class);
        verify(prepareAgentClient).generate(apiCaptor.capture(), any());
        assertThat(apiCaptor.getValue().getPptConfig().getStructure()).contains("# 总结");

        // 转换入参注入 frontmatter + java-uncover 主题样式（<style> 内联，marp2pptx 不支持 --theme-set）
        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        assertThat(mdCaptor.getValue()).startsWith("---\nmarp: true")
                .contains("size: 16:9").contains("<style>").contains("@theme java-uncover").contains("第二页");

        // 落库 md 为智能体返回原始内容（不含 frontmatter）
        ArgumentCaptor<TeachPrepareRecord> recordCaptor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getPptxMd()).doesNotStartWith("---").contains("第二页");
    }

    @Test
    void generate_shouldUseSpecifiedTemplate_whenOwnedTemplateProvided() {
        // 指定本人自定义模板：校验通过并按模板主题注入（java-dark 样式内联）
        TeachPptTemplate own = systemTemplate();
        own.setId(20L);
        own.setThemeCode("java-dark");
        own.setIsSystem(0);
        own.setTeacherId(TEACHER_ID);

        PrepareRequest request = buildRequest();
        request.setTemplateId(20L);
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(own);
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        service.generateTeachingMaterials(request);

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        assertThat(mdCaptor.getValue()).contains("<style>").contains("@theme java-dark");
    }

    @Test
    void generate_shouldInsertStyleAfterFrontmatter_whenAgentMdContainsFrontmatter() {
        // Dify 输出已含 frontmatter：样式块插到闭合线之后、正文之前，不重复注入基础配置
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(systemTemplate()));
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        com.amlve.agentprepareapi.dto.response.PrepareResponse agentResult = buildAgentResult();
        agentResult.setPptx("---\nmarp: true\ntheme: gaia\n---\n\n# PPT\n\n---\n\n# 第二页");
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(agentResult));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        service.generateTeachingMaterials(buildRequest());

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        String md = mdCaptor.getValue();
        String frontmatterHead = "---\nmarp: true\ntheme: gaia\n---";
        assertThat(md).startsWith(frontmatterHead).contains("<style>").contains("@theme java-uncover");
        // 顺序：frontmatter 闭合线 → style 块 → 正文
        int frontmatterEnd = md.indexOf(frontmatterHead) + frontmatterHead.length();
        int stylePos = md.indexOf("<style>");
        int bodyPos = md.indexOf("# PPT");
        assertThat(stylePos).isGreaterThan(frontmatterEnd);
        assertThat(bodyPos).isGreaterThan(stylePos);
    }

    @Test
    void generate_shouldReject_whenTemplateNotAvailable() {
        // 指定模板不存在：备课失败并落失败记录
        PrepareRequest request = buildRequest();
        request.setTemplateId(999L);
        when(teachPptTemplateMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.generateTeachingMaterials(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PPT模板不存在或不可用");

        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(0);
        verify(prepareAgentClient, never()).generate(any(), any());
    }

    @Test
    void generate_shouldReject_whenTemplateBelongsToOtherTeacher() {
        // 指定他人自定义模板：越权拒绝
        TeachPptTemplate others = systemTemplate();
        others.setId(20L);
        others.setIsSystem(0);
        others.setTeacherId(99L);

        PrepareRequest request = buildRequest();
        request.setTemplateId(20L);
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(others);

        assertThatThrownBy(() -> service.generateTeachingMaterials(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PPT模板不存在或不可用");

        verify(prepareAgentClient, never()).generate(any(), any());
    }

    @Test
    void generate_shouldPreferRequestStyleTokens_overTemplateTokens() {
        // 请求显式传 styleTokens：优先于模板主题预置令牌，输出白名单覆写规则
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(systemTemplate()));
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        PrepareRequest request = buildRequest();
        request.setStyleTokens(java.util.Map.of(
                "backgroundColor", "#fafaf9",
                "primaryColor", "#292524",
                "headingWeight", "800"));

        service.generateTeachingMaterials(request);

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        String md = mdCaptor.getValue();
        // 主题样式仍在，令牌覆写使用请求值（而非模板 java-uncover 的默认值）
        assertThat(md).contains("@theme java-uncover")
                .contains("/* design tokens */")
                .contains("background: #fafaf9")
                .contains("font-weight: 800");
    }

    @Test
    void generate_shouldFallbackToTemplateDesignTokens_whenRequestTokensAbsent() {
        // 未传 styleTokens：回退模板 themeCode 对应的预置令牌
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(systemTemplate()));
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        service.generateTeachingMaterials(buildRequest());

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        // java-uncover 预置令牌：浅灰底
        assertThat(mdCaptor.getValue()).contains("/* design tokens */").contains("background: #f5f5f4");
    }

    @Test
    void generate_shouldPreferTemplateDbTokens_overPresetTokens() {
        // 模板库内配置了 design_tokens：优先于 themeCode 预置令牌
        TeachPptTemplate template = systemTemplate();
        template.setDesignTokens(java.util.Map.of(
                "backgroundColor", "#101010",
                "primaryColor", "#fafafa"));
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.List.of(template));
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        service.generateTeachingMaterials(buildRequest());

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        assertThat(mdCaptor.getValue())
                .contains("background: #101010")
                .contains("color: #fafafa");
    }

    @Test
    void generate_shouldDropIllegalStyleTokens() {
        // 非法令牌（CSS 注入/越权字重）逐项丢弃，不影响备课主流程
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class), any()))
                .thenReturn(Result.success(buildAgentResult()));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        mockInsertBackfillId();

        PrepareRequest request = buildRequest();
        request.setStyleTokens(java.util.Map.of(
                "backgroundColor", "red; } body { display: none",
                "fontFamily", "Arial; background: url(http://evil)",
                "headingWeight", "9999"));

        PrepareResponse response = service.generateTeachingMaterials(request);

        assertThat(response.getPptxUrl()).isEqualTo(PPTX_PATH);
        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        // 全部令牌非法：不输出覆写规则，原文不被注入
        assertThat(mdCaptor.getValue())
                .doesNotContain("design tokens")
                .doesNotContain("display: none")
                .doesNotContain("evil");
    }
}
