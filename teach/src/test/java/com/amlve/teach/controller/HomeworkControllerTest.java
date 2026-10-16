package com.amlve.teach.controller;

import com.amlve.teach.config.MethodSecurityTestConfig;
import com.amlve.teach.service.TeachService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 作业接口切片测试。
 * 覆盖：TEACHER 角色放行、STUDENT 角色拦截、匿名拒绝、参数校验。
 */
@WebMvcTest(HomeworkController.class)
@Import(MethodSecurityTestConfig.class)
class HomeworkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachService teachService;

    private static final String PUBLISH_BODY = """
            {
              "title": "第一次Java作业",
              "content": "完成教材第1章课后习题",
              "chapterId": 1,
              "deadline": "2026-09-15T23:59:59"
            }
            """;

    @Test
    @WithMockUser(roles = "TEACHER")
    void publish_shouldSucceed_whenTeacherRole() throws Exception {
        when(teachService.publishHomework(any())).thenReturn(100L);

        mockMvc.perform(post("/api/teach/homework/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(100));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void publish_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(post("/api/teach/homework/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isForbidden());

        verify(teachService, never()).publishHomework(any());
    }

    @Test
    @WithAnonymousUser
    void publish_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/teach/homework/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLISH_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void publish_shouldReject400_whenTitleMissing() throws Exception {
        // 标题缺失，触发 @NotBlank 校验
        String invalidBody = """
                {
                  "content": "完成教材第1章课后习题",
                  "chapterId": 1,
                  "deadline": "2026-09-15T23:59:59"
                }
                """;

        mockMvc.perform(post("/api/teach/homework/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());

        verify(teachService, never()).publishHomework(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_shouldSucceed_whenTeacherRole() throws Exception {
        when(teachService.listMyHomework()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/teach/homework/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void list_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/homework/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_shouldSucceed_whenTeacherRole() throws Exception {
        com.amlve.teach.dto.response.HomeworkDetailResponse detail = new com.amlve.teach.dto.response.HomeworkDetailResponse();
        detail.setId(100L);
        detail.setStatusName("已发布");
        when(teachService.getMyHomeworkDetail(100L)).thenReturn(detail);

        mockMvc.perform(get("/api/teach/homework/detail").param("homeworkId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.statusName").value("已发布"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void detail_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/homework/detail").param("homeworkId", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_shouldReject400_whenHomeworkIdMissing() throws Exception {
        // homeworkId 属于必传请求参数，缺失触发参数解析失败
        mockMvc.perform(get("/api/teach/homework/detail"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void close_shouldSucceed_whenTeacherRole() throws Exception {
        mockMvc.perform(post("/api/teach/homework/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"homeworkId\": 100, \"closeReason\": \"教学调整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(teachService).closeHomework(any());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void close_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(post("/api/teach/homework/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"homeworkId\": 100}"))
                .andExpect(status().isForbidden());

        verify(teachService, never()).closeHomework(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void close_shouldReject400_whenHomeworkIdMissing() throws Exception {
        // homeworkId 缺失，触发 @NotNull 校验
        mockMvc.perform(post("/api/teach/homework/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closeReason\": \"教学调整\"}"))
                .andExpect(status().isBadRequest());

        verify(teachService, never()).closeHomework(any());
    }
}
