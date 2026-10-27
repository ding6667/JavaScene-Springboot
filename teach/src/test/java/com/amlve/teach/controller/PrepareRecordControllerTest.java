package com.amlve.teach.controller;

import com.amlve.common.exception.BusinessException;
import com.amlve.teach.config.MethodSecurityTestConfig;
import com.amlve.teach.dto.response.LessonPlanExportData;
import com.amlve.teach.dto.response.PrepareRecordDetailResponse;
import com.amlve.teach.dto.response.PrepareRecordListResponse;
import com.amlve.teach.service.TeachPrepareRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 备课记录接口切片测试：权限、参数透传、文件流导出与失败 JSON 兜底。
 */
@WebMvcTest(PrepareRecordController.class)
@Import(MethodSecurityTestConfig.class)
class PrepareRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachPrepareRecordService teachPrepareRecordService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_shouldSucceed_whenTeacherRole() throws Exception {
        PrepareRecordListResponse response = new PrepareRecordListResponse();
        response.setTotal(1);
        response.setPageNum(1);
        response.setPageSize(10);
        response.setRecords(Collections.emptyList());
        when(teachPrepareRecordService.listMyRecords(eq(5L), eq(1), eq(10))).thenReturn(response);

        mockMvc.perform(get("/api/teach/prepare/record/list")
                        .param("chapterId", "5")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void list_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/prepare/record/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void detail_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/teach/prepare/record/detail")
                        .param("id", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_shouldPassRecordIdToService() throws Exception {
        PrepareRecordDetailResponse detail = new PrepareRecordDetailResponse();
        detail.setId(100L);
        detail.setLessonPlan("【教案】");
        detail.setTemplateId(null);
        detail.setCreateTime(LocalDateTime.now());
        when(teachPrepareRecordService.getRecordDetail(eq(100L))).thenReturn(detail);

        mockMvc.perform(get("/api/teach/prepare/record/detail")
                        .param("id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.lessonPlan").value("【教案】"));

        verify(teachPrepareRecordService).getRecordDetail(eq(100L));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void status_shouldReturnCode_whenTeacherPollsOwnRecord() throws Exception {
        when(teachPrepareRecordService.getRecordStatus(eq(100L))).thenReturn(2);

        mockMvc.perform(get("/api/teach/prepare/record/status")
                        .param("id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void status_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/prepare/record/status")
                        .param("id", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void export_shouldReturnFileStream_whenValid() throws Exception {
        LessonPlanExportData data = new LessonPlanExportData();
        data.setFileName("备课教案_100.docx");
        data.setContent("docx-bytes".getBytes(StandardCharsets.UTF_8));
        when(teachPrepareRecordService.exportLessonPlan(eq(100L))).thenReturn(data);

        mockMvc.perform(get("/api/teach/prepare/record/export")
                        .param("recordId", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                // 文件名 RFC 5987 编码，中文部分转义
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''%E5%A4%87%E8%AF%BE%E6%95%99%E6%A1%88_100.docx"))
                .andExpect(content().bytes(data.getContent()));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void export_shouldReturnJsonError_whenServiceFails() throws Exception {
        when(teachPrepareRecordService.exportLessonPlan(eq(100L)))
                .thenThrow(new BusinessException("教案内容为空，无法导出"));

        // 文件流接口异常走 Controller 内 JSON 兜底，HTTP 仍为 200，业务错误在 JSON 体中
        mockMvc.perform(get("/api/teach/prepare/record/export")
                        .param("recordId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("教案内容为空，无法导出"));
    }
}
