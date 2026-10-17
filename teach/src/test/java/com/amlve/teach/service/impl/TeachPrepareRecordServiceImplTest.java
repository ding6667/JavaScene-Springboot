package com.amlve.teach.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.FileFormatUtil;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.response.LessonPlanExportData;
import com.amlve.teach.dto.response.PrepareRecordDetailResponse;
import com.amlve.teach.dto.response.PrepareRecordListResponse;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 备课记录查询与教案导出单元测试：验证归属校验、分页映射、材料有值标记、导出兜底。
 */
@ExtendWith(MockitoExtension.class)
class TeachPrepareRecordServiceImplTest {

    @Mock
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Mock
    private FileFormatUtil fileFormatUtil;

    @Mock
    private KnowledgeChapterApi knowledgeChapterApi;

    @InjectMocks
    private TeachPrepareRecordServiceImpl service;

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 99L;
    private static final Long RECORD_ID = 100L;

    @BeforeEach
    void login() {
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher01");
    }

    @AfterEach
    void logout() {
        SecurityContextHolderTestUtil.logout();
    }

    /** 构造一条本人成功备课记录 */
    private TeachPrepareRecord ownedRecord() {
        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setId(RECORD_ID);
        record.setChapterId(5L);
        record.setTeacherId(TEACHER_ID);
        record.setLessonPlan("【教案】面向对象基础");
        record.setKeyPoints("封装、继承、多态");
        record.setHomework("【课后习题】3道");
        record.setLab("");
        record.setPptxMd("# PPT内容");
        record.setPptxUrl("D:/temp/commonFile/xxx.pptx");
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    @Test
    void listMyRecords_shouldMapItems_whenHasRecords() {
        TeachPrepareRecord record = ownedRecord();
        record.setLab("");
        when(teachPrepareRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<TeachPrepareRecord> page = invocation.getArgument(0);
                    page.setTotal(1);
                    page.setRecords(List.of(record));
                    return page;
                });

        PrepareRecordListResponse response = service.listMyRecords(5L, 1, 10);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getRecords()).hasSize(1);
        PrepareRecordListResponse.Item item = response.getRecords().get(0);
        assertThat(item.getId()).isEqualTo(RECORD_ID);
        assertThat(item.getHasLessonPlan()).isTrue();
        assertThat(item.getHasKeyPoints()).isTrue();
        assertThat(item.getHasHomework()).isTrue();
        // lab 为空串：应为 false，且 pptxUrl 有值为 true
        assertThat(item.getHasLab()).isFalse();
        assertThat(item.getHasPpt()).isTrue();
    }

    @Test
    void listMyRecords_shouldReturnEmpty_whenNoRecords() {
        when(teachPrepareRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<TeachPrepareRecord> page = invocation.getArgument(0);
                    page.setTotal(0);
                    page.setRecords(Collections.emptyList());
                    return page;
                });

        PrepareRecordListResponse response = service.listMyRecords(null, 1, 10);

        assertThat(response.getTotal()).isZero();
        assertThat(response.getRecords()).isEmpty();
    }

    @Test
    void getRecordDetail_shouldReturnMaterials_whenOwned() {
        TeachPrepareRecord record = ownedRecord();
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        PrepareRecordDetailResponse response = service.getRecordDetail(RECORD_ID);

        assertThat(response.getLessonPlan()).contains("面向对象基础");
        assertThat(response.getKeyPoints()).contains("多态");
        assertThat(response.getPptxUrl()).endsWith(".pptx");
        // templateId 依赖 teach_ppt 增加 template_id 列，降级方案下应为 null
        assertThat(response.getTemplateId()).isNull();
    }

    @Test
    void getRecordDetail_shouldReject_whenNotOwner() {
        TeachPrepareRecord record = ownedRecord();
        record.setTeacherId(OTHER_TEACHER_ID);
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        // 越权访问他人备课记录：必须抛业务异常
        assertThatThrownBy(() -> service.getRecordDetail(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问");
    }

    @Test
    void getRecordDetail_shouldReject_whenNotFound() {
        when(teachPrepareRecordMapper.selectById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> service.getRecordDetail(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void exportLessonPlan_shouldReject_whenLessonPlanEmpty() {
        TeachPrepareRecord record = ownedRecord();
        record.setLessonPlan("");
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        assertThatThrownBy(() -> service.exportLessonPlan(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("教案内容为空");
    }

    @Test
    void exportLessonPlan_shouldReturnWordBytes_whenValid() {
        TeachPrepareRecord record = ownedRecord();
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);
        byte[] wordBytes = new byte[]{1, 2, 3, 4};
        when(fileFormatUtil.generateWord(record.getLessonPlan())).thenReturn(wordBytes);
        // 注入临时留档目录，验证留档不阻断导出
        ReflectionTestUtils.setField(service, "commonFileDir", tempDir.toString());

        LessonPlanExportData data = service.exportLessonPlan(RECORD_ID);

        assertThat(data.getContent()).isSameAs(wordBytes);
        // 章节名查询未打桩返回 null：降级保底 备课教案_{recordId}.docx
        assertThat(data.getFileName()).isEqualTo("备课教案_" + RECORD_ID + ".docx");
    }

    @Test
    void exportLessonPlan_shouldUseChapterShortNameInFileName() {
        // 章节名命中：导出文件名为 {章节短名}_教案.docx，满足验收标准
        TeachPrepareRecord record = ownedRecord();
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);
        when(fileFormatUtil.generateWord(record.getLessonPlan())).thenReturn(new byte[]{1});
        ReflectionTestUtils.setField(service, "commonFileDir", tempDir.toString());
        ChapterNameDTO chapterName = new ChapterNameDTO();
        chapterName.setChapterId(record.getChapterId());
        chapterName.setChapterName("第 2 章 Java 编程基础");
        chapterName.setShortName("Java 编程基础");
        when(knowledgeChapterApi.getChapterNameById(record.getChapterId()))
                .thenReturn(Result.success(chapterName));

        LessonPlanExportData data = service.exportLessonPlan(RECORD_ID);

        // 章节名中的空格属非法文件名字符，替换为下划线
        assertThat(data.getFileName()).isEqualTo("Java_编程基础_教案.docx");
    }
}
