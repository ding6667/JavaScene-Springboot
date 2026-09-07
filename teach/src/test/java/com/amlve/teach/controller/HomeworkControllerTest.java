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
}
