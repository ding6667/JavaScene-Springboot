package com.amlve.teach.controller;

import com.amlve.agentprepare.client.PrepareClient;
import com.amlve.teach.config.MethodSecurityTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
    private PrepareClient prepareClient;

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldSucceed_whenTeacherRole() throws Exception {
        when(prepareClient.generateTeachingPlan(5L)).thenReturn("【教案模板 - 第5章】");

        mockMvc.perform(post("/api/agent/teach/prepare").param("chapterId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("【教案模板 - 第5章】"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void prepare_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(post("/api/agent/teach/prepare").param("chapterId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void prepare_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/agent/teach/prepare").param("chapterId", "5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldReject400_whenChapterIdMissing() throws Exception {
        mockMvc.perform(post("/api/agent/teach/prepare"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldPassChapterIdToClient() throws Exception {
        when(prepareClient.generateTeachingPlan(5L)).thenReturn("教案");

        mockMvc.perform(post("/api/agent/teach/prepare").param("chapterId", "5"))
                .andExpect(status().isOk());

        verify(prepareClient).generateTeachingPlan(5L);
    }
}
