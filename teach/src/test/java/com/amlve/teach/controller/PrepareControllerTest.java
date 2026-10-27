package com.amlve.teach.controller;

import com.amlve.teach.config.MethodSecurityTestConfig;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.service.TeachPrepareService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 备课智能体接口切片测试（Controller 只做参数接收与响应，业务在 Service）。
 */
@WebMvcTest(PrepareController.class)
@Import(MethodSecurityTestConfig.class)
class PrepareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeachPrepareService teachPrepareService;

    /** 构造 Service Mock 受理响应：只有记录ID与状态，五类材料需轮询详情接口 */
    private PrepareResponse mockResponse() {
        PrepareResponse response = new PrepareResponse();
        response.setChapterId(5L);
        response.setRecordId(100L);
        response.setStatus(2);
        response.setStatusDesc("生成中");
        response.setGenerateTime(LocalDateTime.now());
        return response;
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void prepare_shouldSucceed_whenTeacherRole() throws Exception {
        when(teachPrepareService.generateTeachingMaterials(any(PrepareRequest.class)))
                .thenReturn(mockResponse());

        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.chapterId").value(5))
                .andExpect(jsonPath("$.data.recordId").value(100))
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.statusDesc").value("生成中"));
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
    void prepare_shouldPassChapterIdToService() throws Exception {
        when(teachPrepareService.generateTeachingMaterials(any(PrepareRequest.class)))
                .thenReturn(mockResponse());

        mockMvc.perform(post("/api/agent/teach/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chapterId\":5,\"userMessage\":\"侧重案例讲解\"}"))
                .andExpect(status().isOk());

        // 验证章节ID与用户额外指令被正确传递给 Service
        ArgumentCaptor<PrepareRequest> captor = ArgumentCaptor.forClass(PrepareRequest.class);
        verify(teachPrepareService).generateTeachingMaterials(captor.capture());
        assertEquals(5L, captor.getValue().getChapterId().longValue());
        assertEquals("侧重案例讲解", captor.getValue().getUserMessage());
    }
}
