package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.response.LessonPlanExportData;
import com.amlve.teach.dto.response.PrepareRecordDetailResponse;
import com.amlve.teach.dto.response.PrepareRecordListResponse;
import com.amlve.teach.service.TeachPrepareRecordService;
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
 * 备课记录查询与教案导出接口（教师端）
 */
@Slf4j
@RestController
@RequestMapping("/api/teach/prepare/record")
@RequiredArgsConstructor
public class PrepareRecordController {

    private final TeachPrepareRecordService teachPrepareRecordService;
    private final ObjectMapper objectMapper;

    /** 备课记录分页列表（本人成功记录，chapterId 可选筛选） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/list")
    public Result<PrepareRecordListResponse> list(@RequestParam(required = false) Long chapterId,
                                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(teachPrepareRecordService.listMyRecords(chapterId, pageNum, pageSize));
    }

    /** 备课记录详情（五类材料全文） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/detail")
    public Result<PrepareRecordDetailResponse> detail(@RequestParam Long id) {
        return Result.success(teachPrepareRecordService.getRecordDetail(id));
    }

    /**
     * 备课记录状态轮询：data 为状态码（2=生成中，1=成功，0=失败）
     * <p>
     * 备课接口秒回 recordId 后，前端按此接口轮询到非 2 再去取 /detail。
     */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/status")
    public Result<Integer> status(@RequestParam Long id) {
        return Result.success(teachPrepareRecordService.getRecordStatus(id));
    }

    /** 教案导出 Word 文件流（.docx） */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/export")
    public void export(@RequestParam Long recordId, HttpServletResponse response) throws IOException {
        try {
            LessonPlanExportData data = teachPrepareRecordService.exportLessonPlan(recordId);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            String encoded = URLEncoder.encode(data.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            response.setContentLength(data.getContent().length);
            response.getOutputStream().write(data.getContent());
        } catch (Exception e) {
            // 文件流接口无法走全局异常处理器，失败统一返回 JSON
            log.warn("教案导出失败: recordId={}", recordId, e);
            response.reset();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.error(e.getMessage())));
        }
    }
}
