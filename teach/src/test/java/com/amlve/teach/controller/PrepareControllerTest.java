package com.amlve.teach.controller;

import com.amlve.agentprepareapi.PrepareAgentClient;
import com.amlve.teach.config.MethodSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 备课智能体接口切片测试。
 */
@WebMvcTest(PrepareController.class)
@Import(MethodSecurityTestConfig.class)
class PrepareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrepareAgentClient prepareAgentClient;

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldSucceed_whenTeacherRole() throws Exception {
        when(prepareAgentClient.getLessonText("第5章")).thenReturn("【教案模板 - 第5章】");

        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chapterId").value(5))
                .andExpect(jsonPath("$.data.teachingPlan").value("【教案模板 - 第5章】"))
                .andExpect(jsonPath("$.data.generateTime").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void prepare_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void prepare_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldReject400_whenChapterIdMissing() throws Exception {
        // chapterId 为 null，@NotNull 校验应返回 400
        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldReject400_whenBodyMalformed() throws Exception {
        // 请求体非法 JSON 应返回 400
        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldPassChapterIdToClient() throws Exception {
        when(prepareAgentClient.getLessonText(anyString())).thenReturn("教案");

        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5}"))
                .andExpect(status().isOk());

        // 验证章节ID被正确组装为提示词传给智能体客户端
        verify(prepareAgentClient).getLessonText("第5章");
    }
}
