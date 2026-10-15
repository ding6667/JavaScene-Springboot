package com.amlve.teach.controller;

import com.amlve.common.exception.BusinessException;
import com.amlve.teach.config.MethodSecurityTestConfig;
import com.amlve.teach.dto.response.PptDownloadData;
import com.amlve.teach.service.PptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PPT 下载接口切片测试：权限、文件流响应与失败 JSON 兜底。
 */
@WebMvcTest(PptController.class)
@Import(MethodSecurityTestConfig.class)
class PptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PptService pptService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void download_shouldReturnFileStream_whenValid() throws Exception {
        PptDownloadData data = new PptDownloadData();
        data.setFileName("第5章课件.pptx");
        data.setContent("pptx-bytes".getBytes(StandardCharsets.UTF_8));
        when(pptService.downloadPpt(eq(100L))).thenReturn(data);

        mockMvc.perform(get("/api/teach/ppt/download")
                        .param("recordId", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(content().bytes(data.getContent()));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void download_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/ppt/download")
                        .param("recordId", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void download_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/teach/ppt/download")
                        .param("recordId", "100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void download_shouldReturnJsonError_whenFileExpired() throws Exception {
        when(pptService.downloadPpt(eq(100L)))
                .thenThrow(new BusinessException("文件已过期，请重新生成"));

        // 文件流接口异常走 Controller 内 JSON 兜底，业务错误在 JSON 体中
        mockMvc.perform(get("/api/teach/ppt/download")
                        .param("recordId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("文件已过期，请重新生成"));
    }
}
