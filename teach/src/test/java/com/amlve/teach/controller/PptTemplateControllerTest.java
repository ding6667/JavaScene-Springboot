package com.amlve.teach.controller;

import com.amlve.teach.config.MethodSecurityTestConfig;
import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.MarpThemeVO;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;
import com.amlve.teach.service.PptTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PPT 模板接口切片测试：权限控制、常量主题返回、保存参数校验。
 */
@WebMvcTest(PptTemplateController.class)
@Import(MethodSecurityTestConfig.class)
class PptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PptTemplateService pptTemplateService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void themes_shouldReturnFourThemes() throws Exception {
        MarpThemeVO theme = new MarpThemeVO();
        theme.setCode("java-default");
        theme.setName("标准教学蓝");
        theme.setDescription("白底深蓝主色");
        when(pptTemplateService.listThemes()).thenReturn(List.of(theme));

        mockMvc.perform(get("/api/teach/ppt/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].code").value("java-default"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_shouldReturnTemplates() throws Exception {
        PptTemplateResponse item = new PptTemplateResponse();
        item.setId(10L);
        item.setName("Java标准教学");
        item.setThemeCode("java-default");
        item.setIsSystem(1);
        when(pptTemplateService.listTemplates()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/teach/ppt/template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Java标准教学"))
                .andExpect(jsonPath("$.data[0].isSystem").value(1));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void detail_shouldReturnStructureMd() throws Exception {
        PptTemplateDetailVO vo = new PptTemplateDetailVO();
        vo.setId(10L);
        vo.setName("Java标准教学");
        vo.setStructureMd("# 骨架全文");
        when(pptTemplateService.getTemplateDetail(10L)).thenReturn(vo);

        mockMvc.perform(get("/api/teach/ppt/template/detail/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.structureMd").value("# 骨架全文"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void save_shouldReturnTemplateId() throws Exception {
        when(pptTemplateService.saveTemplate(any(PptTemplateSaveRequest.class))).thenReturn(77L);

        mockMvc.perform(post("/api/teach/ppt/template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"我的模板\",\"themeCode\":\"java-default\",\"structureMd\":\"# 骨架\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(77));

        verify(pptTemplateService).saveTemplate(any(PptTemplateSaveRequest.class));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void save_shouldReject400_whenThemeCodeNotPreset() throws Exception {
        // themeCode 不在 4 套预置主题内：参数校验拦截
        mockMvc.perform(post("/api/teach/ppt/template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"我的模板\",\"themeCode\":\"java-neon\",\"structureMd\":\"# 骨架\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void save_shouldReject400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/teach/ppt/template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"themeCode\":\"java-default\",\"structureMd\":\"# 骨架\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void list_shouldReject403_whenStudentRole() throws Exception {
        mockMvc.perform(get("/api/teach/ppt/template/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void themes_shouldReject401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/teach/ppt/template"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void list_shouldReturnEmptyList_whenNoTemplates() throws Exception {
        when(pptTemplateService.listTemplates()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/teach/ppt/template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
