package com.amlve.teach.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.teach.dto.response.PptDownloadData;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PPT 下载单元测试：验证归属校验、文件缺失兜底、下载计数与文件名回退。
 */
@ExtendWith(MockitoExtension.class)
class PptServiceImplTest {

    @Mock
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Mock
    private TeachPptMapper teachPptMapper;

    @InjectMocks
    private PptServiceImpl service;

    @TempDir
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

    /** 构造本人备课记录，pptxUrl 指向临时目录下真实文件 */
    private TeachPrepareRecord ownedRecordWithFile(String fileName) throws Exception {
        Path pptFile = tempDir.resolve(fileName);
        Files.write(pptFile, new byte[]{9, 9, 9});

        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setId(RECORD_ID);
        record.setTeacherId(TEACHER_ID);
        record.setPptxUrl(pptFile.toString());
        return record;
    }

    @Test
    void downloadPpt_shouldReject_whenNotOwner() {
        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setTeacherId(OTHER_TEACHER_ID);
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        // 越权下载他人 PPT：必须拒绝
        assertThatThrownBy(() -> service.downloadPpt(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有访问权限");
    }

    @Test
    void downloadPpt_shouldReject_whenPptNotGenerated() {
        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setTeacherId(TEACHER_ID);
        record.setPptxUrl(null);
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        assertThatThrownBy(() -> service.downloadPpt(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没有生成 PPT");
    }

    @Test
    void downloadPpt_shouldReject_whenFileMissing() {
        TeachPrepareRecord record = new TeachPrepareRecord();
        record.setTeacherId(TEACHER_ID);
        record.setPptxUrl(tempDir.resolve("deleted.pptx").toString());
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        assertThatThrownBy(() -> service.downloadPpt(RECORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件链接已过期");
    }

    @Test
    void downloadPpt_shouldSucceedAndIncrementCount_whenFileExists() throws Exception {
        TeachPrepareRecord record = ownedRecordWithFile("lesson.pptx");
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        TeachPpt ppt = new TeachPpt();
        ppt.setFileName("第5章课件.pptx");
        when(teachPptMapper.selectOne(any())).thenReturn(ppt);
        // update 返回受影响行数
        when(teachPptMapper.update(any(), any(Wrapper.class))).thenReturn(1);

        PptDownloadData data = service.downloadPpt(RECORD_ID);

        // 文件名取自 teach_ppt.file_name
        assertThat(data.getFileName()).isEqualTo("第5章课件.pptx");
        assertThat(data.getContent()).hasSize(3);
        // 下载计数自增：确认按 file_path 条件执行了 update
        verify(teachPptMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void downloadPpt_shouldFallbackFileName_whenPptRecordMissing() throws Exception {
        TeachPrepareRecord record = ownedRecordWithFile("lesson.pptx");
        when(teachPrepareRecordMapper.selectById(RECORD_ID)).thenReturn(record);
        when(teachPptMapper.selectOne(any())).thenReturn(null);
        when(teachPptMapper.update(any(), any(Wrapper.class))).thenReturn(0);

        PptDownloadData data = service.downloadPpt(RECORD_ID);

        // teach_ppt 无记录时文件名兜底
        assertThat(data.getFileName()).isEqualTo("课件_" + RECORD_ID + ".pptx");
    }
}
