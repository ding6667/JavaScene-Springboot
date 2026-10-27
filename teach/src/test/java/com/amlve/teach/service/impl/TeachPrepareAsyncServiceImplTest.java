package com.amlve.teach.service.impl;

import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import com.amlve.agentprepareapi.service.PrepareAgentClient;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPptThemeMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPptTheme;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.amlve.teach.util.MarkdownToPptxUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 备课后台生成单元测试：Mock 智能体客户端/PPT工具/Mapper，
 * 验证材料回写（status=1）、PPT 记录落库、主题注入与失败终态（status=0 + error_msg）。
 * <p>
 * 受理阶段（模板校验、占位记录落库、异步交接）见 {@link TeachPrepareServiceImplTest}。
 */
@ExtendWith(MockitoExtension.class)
class TeachPrepareAsyncServiceImplTest {

    @Mock
    private PrepareAgentClient prepareAgentClient;

    @Mock
    private MarkdownToPptxUtil markdownToPptxUtil;

    @Mock
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Mock
    private TeachPptMapper teachPptMapper;

    @Mock
    private TeachPptThemeMapper teachPptThemeMapper;

    @InjectMocks
    private TeachPrepareAsyncServiceImpl service;

    private static final Long RECORD_ID = 100L;
    private static final Long CHAPTER_ID = 5L;
    private static final Long TEACHER_ID = 1L;
    private static final String PPTX_PATH = "D:/temp/commonFile/2026-09-08/uuid.pptx";

    private PrepareRequest buildAgentRequest() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setUserMessage("侧重案例讲解");
        return request;
    }

    private TeachingMaterialDTO buildMaterial() {
        return TeachingMaterialDTO.builder().chapterSummary("本章介绍类与对象").build();
    }

    /** 智能体返回（含 PPT Markdown） */
    private PrepareResponse buildAgentResult() {
        PrepareResponse result = new PrepareResponse();
        result.setLessonPlan("## 教案");
        result.setPptx("# PPT\n\n---\n\n# 第二页");
        result.setKeyPoints("## 重难点");
        result.setHomework("## 习题");
        result.setLab("## 实训");
        return result;
    }

    /** 库内主题（CSS 由 teach_ppt_theme 存表，不再读 classpath 文件） */
    private TeachPptTheme theme(String themeCode) {
        TeachPptTheme theme = new TeachPptTheme();
        theme.setThemeCode(themeCode);
        theme.setCssText("@theme " + themeCode + "\nsection { background: #fff; }");
        return theme;
    }

    private void mockGenerateSuccess(PrepareResponse agentResult) {
        when(prepareAgentClient.generate(any(PrepareRequest.class), any(TeachingMaterialDTO.class)))
                .thenReturn(Result.success(agentResult));
    }

    /** 捕获回写备课记录的实体 */
    private TeachPrepareRecord capturedRecordUpdate() {
        ArgumentCaptor<TeachPrepareRecord> captor = ArgumentCaptor.forClass(TeachPrepareRecord.class);
        verify(teachPrepareRecordMapper).updateById(captor.capture());
        return captor.getValue();
    }

    @Test
    void generate_shouldWriteBackMaterials_andInsertPpt_whenSuccess() {
        mockGenerateSuccess(buildAgentResult());
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        // 按受理时的记录ID回写，status=1；不新增记录
        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getId()).isEqualTo(RECORD_ID);
        assertThat(update.getStatus()).isEqualTo(1);
        assertThat(update.getLessonPlan()).isEqualTo("## 教案");
        assertThat(update.getKeyPoints()).isEqualTo("## 重难点");
        assertThat(update.getHomework()).isEqualTo("## 习题");
        assertThat(update.getLab()).isEqualTo("## 实训");
        assertThat(update.getPptxMd()).contains("第二页");
        assertThat(update.getPptxUrl()).isEqualTo(PPTX_PATH);
        verify(teachPrepareRecordMapper, never()).insert(any(TeachPrepareRecord.class));

        // PPT 记录落库字段；文件在测试环境不存在，fileSize 降级为 null 不影响其余字段
        ArgumentCaptor<TeachPpt> pptCaptor = ArgumentCaptor.forClass(TeachPpt.class);
        verify(teachPptMapper).insert(pptCaptor.capture());
        TeachPpt ppt = pptCaptor.getValue();
        assertThat(ppt.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(ppt.getFileName()).isEqualTo("uuid.pptx");
        assertThat(ppt.getFilePath()).isEqualTo(PPTX_PATH);
        assertThat(ppt.getMdContent()).contains("第二页");
        assertThat(ppt.getDownloadCount()).isEqualTo(0);

        // PPT 文件记录必须先落，再翻成功状态（下载接口按 file_path 匹配 teach_ppt）
        org.mockito.InOrder inOrder = inOrder(teachPptMapper, teachPrepareRecordMapper);
        inOrder.verify(teachPptMapper).insert(any(TeachPpt.class));
        inOrder.verify(teachPrepareRecordMapper).updateById(any(TeachPrepareRecord.class));
    }

    @Test
    void generate_shouldMarkFailedInsteadOfSuccess_whenPptRecordInsertFails() {
        // PPT 文件记录没落成，就不能把备课记录标成成功：全程只允许失败终态那一次回写
        mockGenerateSuccess(buildAgentResult());
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);
        when(teachPptMapper.insert(any(TeachPpt.class))).thenThrow(new BusinessException("teach_ppt 落库失败"));

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getStatus()).isEqualTo(0);
        assertThat(update.getErrorMsg()).contains("teach_ppt 落库失败");
        assertThat(update.getLessonPlan()).isNull();
    }

    @Test
    void generate_shouldNotTouchPpt_whenAgentReturnsNoPptx() {
        PrepareResponse agentResult = buildAgentResult();
        agentResult.setPptx(null);
        mockGenerateSuccess(agentResult);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        verify(markdownToPptxUtil, never()).convertContent(anyString());
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getPptxUrl()).isNull();
        assertThat(update.getStatus()).isEqualTo(1);
        assertThat(update.getLessonPlan()).isEqualTo("## 教案");
    }

    @Test
    void generate_shouldDegrade_whenPptxConvertFail() {
        // marp2pptx 不可用：pptxUrl=null，其余材料照常回写
        mockGenerateSuccess(buildAgentResult());
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(null);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getStatus()).isEqualTo(1);
        assertThat(update.getPptxUrl()).isNull();
        assertThat(update.getLessonPlan()).isEqualTo("## 教案");
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
    }

    @Test
    void generate_shouldInjectTheme_whenMdHasNoFrontmatter() {
        mockGenerateSuccess(buildAgentResult());
        when(teachPptThemeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(theme("java-uncover"));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), "java-uncover");

        // 转换入参注入基础 frontmatter + 库内主题样式（<style> 内联，marp2pptx 不支持 --theme-set）
        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        assertThat(mdCaptor.getValue()).startsWith("---\nmarp: true")
                .contains("size: 16:9").contains("<style>").contains("@theme java-uncover").contains("第二页");

        // 落库 md 为智能体返回原始内容（不含 frontmatter 与样式块）
        assertThat(capturedRecordUpdate().getPptxMd()).doesNotStartWith("---").contains("第二页");
    }

    @Test
    void generate_shouldInsertStyleAfterFrontmatter_whenAgentMdContainsFrontmatter() {
        // Dify 输出已含 frontmatter：样式块插到闭合线之后、正文之前，不重复注入基础配置
        PrepareResponse agentResult = buildAgentResult();
        agentResult.setPptx("---\nmarp: true\ntheme: gaia\n---\n\n# PPT\n\n---\n\n# 第二页");
        mockGenerateSuccess(agentResult);
        when(teachPptThemeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(theme("java-uncover"));
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), "java-uncover");

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
    void generate_shouldDegradeWithoutStyle_whenThemeRowMissing() {
        // 主题码在 teach_ppt_theme 查不到：降级为不注入样式，材料照常回写成功
        mockGenerateSuccess(buildAgentResult());
        when(teachPptThemeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(markdownToPptxUtil.convertContent(anyString())).thenReturn(PPTX_PATH);

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), "java-dark");

        ArgumentCaptor<String> mdCaptor = ArgumentCaptor.forClass(String.class);
        verify(markdownToPptxUtil).convertContent(mdCaptor.capture());
        assertThat(mdCaptor.getValue()).doesNotContain("<style>").contains("第二页");
        assertThat(capturedRecordUpdate().getStatus()).isEqualTo(1);
    }

    @Test
    void generate_shouldMarkFailed_whenAgentThrows() {
        when(prepareAgentClient.generate(any(PrepareRequest.class), any(TeachingMaterialDTO.class)))
                .thenThrow(new BusinessException("Dify 备课工作流执行失败: status=failed"));

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        // 异步异常不外抛，改为把记录置为失败终态
        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getId()).isEqualTo(RECORD_ID);
        assertThat(update.getStatus()).isEqualTo(0);
        assertThat(update.getErrorMsg()).contains("Dify");
        verify(teachPptMapper, never()).insert(any(TeachPpt.class));
    }

    @Test
    void generate_shouldMarkFailed_whenAgentReturnsError() {
        when(prepareAgentClient.generate(any(PrepareRequest.class), any(TeachingMaterialDTO.class)))
                .thenReturn(Result.error("工作流执行失败"));

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        TeachPrepareRecord update = capturedRecordUpdate();
        assertThat(update.getStatus()).isEqualTo(0);
        assertThat(update.getErrorMsg()).contains("备课智能体调用失败");
        verify(markdownToPptxUtil, never()).convertContent(anyString());
    }

    @Test
    void generate_shouldTruncateLongErrorMsg() {
        String longMessage = "备课智能体调用失败: ".repeat(60);
        when(prepareAgentClient.generate(any(PrepareRequest.class), any(TeachingMaterialDTO.class)))
                .thenReturn(Result.error(longMessage));

        service.generateAsync(RECORD_ID, CHAPTER_ID, TEACHER_ID, buildAgentRequest(), buildMaterial(), null);

        assertThat(capturedRecordUpdate().getErrorMsg()).hasSize(500);
    }
}
