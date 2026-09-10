package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.PrepareAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.util.MarkdownToPptxUtil;
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
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(buildKnowledgeMaterial());
        when(prepareAgentClient.generate(any(com.amlve.agentprepareapi.dto.request.PrepareRequest.class),
                any(com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO.class)))
                .thenReturn(buildAgentResult());
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
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(buildKnowledgeMaterial());
        when(prepareAgentClient.generate(any(), any())).thenReturn(agentResult);
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
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(buildKnowledgeMaterial());
        when(prepareAgentClient.generate(any(), any())).thenReturn(buildAgentResult());
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
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(buildKnowledgeMaterial());
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
}
