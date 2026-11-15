package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.response.PptDownloadData;
import com.amlve.teach.service.PptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 备课成果 PPT 下载接口（教师端）
 */
@Slf4j
@RestController
@RequestMapping("/api/teach/ppt")
@RequiredArgsConstructor
public class PptController {

    private final PptService pptService;
    private final ObjectMapper objectMapper;

    /** 下载本人备课生成的 PPT 文件（成功后下载计数自增） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/download")
    public void download(@RequestParam Long recordId, HttpServletResponse response) throws IOException {
        try {
            PptDownloadData data = pptService.downloadPpt(recordId);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            String encoded = URLEncoder.encode(data.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            response.setContentLength(data.getContent().length);
            response.getOutputStream().write(data.getContent());
        } catch (Exception e) {
            // 文件流接口无法走全局异常处理器，失败统一返回 JSON
            log.warn("PPT下载失败: recordId={}", recordId, e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(e.getMessage())));
        }
    }
}
