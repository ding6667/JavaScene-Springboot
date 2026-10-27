package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.TeachPrepareAsyncService;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 备课受理阶段单元测试：Mock 素材接口/模板 Mapper/备课记录 Mapper/异步生成服务，
 * 验证模板与素材校验、status=2 记录落库、记录ID 回填与异步交接参数。
 * <p>
 * 智能体调用、PPT 转换与材料回写见 {@link TeachPrepareAsyncServiceImplTest}。
 */
@ExtendWith(MockitoExtension.class)
class TeachPrepareServiceImplTest {

    @Mock
    private KnowledgeChapterApi knowledgeChapterApi;

    @Mock
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Mock
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @Mock
    private TeachPrepareAsyncService teachPrepareAsyncService;

    @InjectMocks
    private TeachPrepareServiceImpl service;

    private static final Long TEACHER_ID = 1L;
    private static final Long CHAPTER_ID = 5L;
    private static final Long RECORD_ID = 100L;

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

    /** 模拟 MyBatis-Plus ASSIGN_ID 主键回填与 MetaObjectHandler 的 createTime 填充 */
    private void mockInsertBackfill() {
        doAnswer(invocation -> {
            TeachPrepareRecord r = invocation.getArgument(0);
            r.setId(RECORD_ID);
            r.setCreateTime(LocalDateTime.now());
            return 1;
        }).when(teachPrepareRecordMapper).insert(any(TeachPrepareRecord.class));
    }

    @Test
    void accept_shouldInsertGeneratingRecord_andReturnRecordId() {
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(systemTemplate()));
        mockInsertBackfill();

        PrepareResponse response = service.generateTeachingMaterials(buildRequest());

        // 受理响应：只有记录ID与状态，不含五类材料
        assertThat(response.getRecordId()).isEqualTo(RECORD_ID);
        assertThat(response.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(response.getStatus()).isEqualTo(2);
        assertThat(response.getStatusDesc()).isEqualTo("生成中");
        assertThat(response.getGenerateTime()).isNotNull();

        // 占位记录：主键雪花ID、教师ID 从登录上下文注入、status=2，材料字段留空待回写
        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).insert(captor.capture());
        TeachPrepareRecord record = captor.getValue();
        assertThat(record.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(record.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(record.getStatus()).isEqualTo(2);
        assertThat(record.getLessonPlan()).isNull();
        assertThat(record.getPptxUrl()).isNull();
    }

    @Test
    void accept_shouldHandOffToAsyncWorker_withPreparedRequestAndThemeCode() {
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(systemTemplate()));
        mockInsertBackfill();

        service.generateTeachingMaterials(buildRequest());

        ArgumentCaptor<com.amlve.agentprepareapi.dto.request.PrepareRequest> apiCaptor =
                ArgumentCaptor.forClass(com.amlve.agentprepareapi.dto.request.PrepareRequest.class);
        ArgumentCaptor<TeachingMaterialDTO> materialCaptor = ArgumentCaptor.forClass(TeachingMaterialDTO.class);
        ArgumentCaptor<String> themeCaptor = ArgumentCaptor.forClass(String.class);
        verify(teachPrepareAsyncService).generateAsync(eq(RECORD_ID), eq(CHAPTER_ID), eq(TEACHER_ID),
                apiCaptor.capture(), materialCaptor.capture(), themeCaptor.capture());

        // 模板骨架随 ppt_config 下发、主题码交后台注入、素材逐字段拷贝
        assertThat(apiCaptor.getValue().getPptConfig().getStructure()).contains("# 总结");
        assertThat(apiCaptor.getValue().getUserMessage()).isEqualTo("侧重案例讲解");
        assertThat(themeCaptor.getValue()).isEqualTo("java-uncover");
        TeachingMaterialDTO material = materialCaptor.getValue();
        assertThat(material.getChapterSummary()).isEqualTo("本章介绍类与对象");
        assertThat(material.getConceptDefinitions()).isEqualTo("[{\"name\":\"类\"}]");
        assertThat(material.getLabTasks()).isEqualTo("[]");
    }

    @Test
    void accept_shouldUseSpecifiedOwnedTemplate() {
        TeachPptTemplate own = systemTemplate();
        own.setId(20L);
        own.setThemeCode("java-dark");
        own.setIsSystem(0);
        own.setTeacherId(TEACHER_ID);

        PrepareRequest request = buildRequest();
        request.setTemplateId(20L);
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(own);
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        mockInsertBackfill();

        service.generateTeachingMaterials(request);

        ArgumentCaptor<String> themeCaptor = ArgumentCaptor.forClass(String.class);
        verify(teachPrepareAsyncService).generateAsync(eq(RECORD_ID), eq(CHAPTER_ID), eq(TEACHER_ID),
                any(), any(), themeCaptor.capture());
        assertThat(themeCaptor.getValue()).isEqualTo("java-dark");
    }

    @Test
    void accept_shouldDegradeToNoTemplate_whenNoSystemTemplate() {
        // 系统模板未初始化：仍受理，骨架下发空串、主题码为 null（后台不注入样式）
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(buildKnowledgeMaterial()));
        mockInsertBackfill();

        PrepareResponse response = service.generateTeachingMaterials(buildRequest());

        assertThat(response.getRecordId()).isEqualTo(RECORD_ID);
        ArgumentCaptor<com.amlve.agentprepareapi.dto.request.PrepareRequest> apiCaptor =
                ArgumentCaptor.forClass(com.amlve.agentprepareapi.dto.request.PrepareRequest.class);
        ArgumentCaptor<String> themeCaptor = ArgumentCaptor.forClass(String.class);
        verify(teachPrepareAsyncService).generateAsync(eq(RECORD_ID), eq(CHAPTER_ID), eq(TEACHER_ID),
                apiCaptor.capture(), any(), themeCaptor.capture());
        assertThat(apiCaptor.getValue().getPptConfig().getStructure()).isEmpty();
        assertThat(themeCaptor.getValue()).isNull();
    }

    @Test
    void accept_shouldRejectWithoutRecord_whenTemplateNotAvailable() {
        PrepareRequest request = buildRequest();
        request.setTemplateId(999L);
        when(teachPptTemplateMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.generateTeachingMaterials(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PPT模板不存在或不可用");

        // 校验在落库之前：不留下半成品记录，也不交接后台任务
        verify(teachPrepareRecordMapper, never()).insert(any(TeachPrepareRecord.class));
        verify(teachPrepareAsyncService, never())
                .generateAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    void accept_shouldRejectWithoutRecord_whenTemplateBelongsToOtherTeacher() {
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

        verify(teachPrepareRecordMapper, never()).insert(any(TeachPrepareRecord.class));
    }

    @Test
    void accept_shouldRejectWithoutRecord_whenMaterialQueryThrows() {
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenThrow(new BusinessException("章节不存在"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("章节不存在");

        verify(teachPrepareRecordMapper, never()).insert(any(TeachPrepareRecord.class));
        verify(teachPrepareAsyncService, never())
                .generateAsync(any(), any(), any(), any(), any(), any());
    }

    @Test
    void accept_shouldRejectWithoutRecord_whenMaterialQueryReturnsError() {
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenReturn(Result.error("章节不存在"));

        assertThatThrownBy(() -> service.generateTeachingMaterials(buildRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查询备课素材失败");

        verify(teachPrepareRecordMapper, never()).insert(any(TeachPrepareRecord.class));
    }
}
