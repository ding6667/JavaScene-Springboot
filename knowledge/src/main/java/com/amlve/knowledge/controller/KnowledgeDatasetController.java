package com.amlve.knowledge.controller;

import com.amlve.common.request.Result;
import com.amlve.knowledge.dto.request.UpdateMetadataRequest;
import com.amlve.knowledge.dto.request.UploadTextRequest;
import com.amlve.knowledge.dto.response.KnowledgeResponse;
import com.amlve.knowledge.dto.response.UploadResponse;
import com.amlve.knowledge.service.KnowledgeDatasetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 知识库数据集接口（检索 / SSE 检索 / 上传 / 元数据）
 * <p>
 * 角色口径与服务层编程式校验一致：检索面向学生，写操作面向教师（教材录入）。
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/dataset")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class KnowledgeDatasetController {

    /**
     * SSE 推送线程池：检索在独立线程执行，Controller 立即返回 emitter 不占用请求主线程
     */
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "knowledge-sse");
        thread.setDaemon(true);
        return thread;
    });

    private final KnowledgeDatasetService knowledgeDatasetService;

    /**
     * 知识点检索（Dify 混合检索）
     * <p>
     * GET /api/knowledge/dataset/query?keywords=什么是面向对象
     */
    @GetMapping("/query")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<KnowledgeResponse> query(@RequestParam String keywords) {
        return Result.success(knowledgeDatasetService.query(keywords));
    }

    /**
     * 知识点检索 SSE 流式接口（对应测试报告 S-06）
     * <p>
     * GET /api/knowledge/dataset/querySSE?keywords=详细讲解为什么数组索引从0开始
     * <p>
     * 事件流：item（每条检索命中一条，JSON 数据）→ done（携带命中总数后结束）；
     * 出错时发送 error 事件后结束。前端用 EventSource 监听（仅支持 GET，与本接口匹配）。
     */
    @GetMapping("/querySSE")
    @PreAuthorize("hasRole('STUDENT')")
    public SseEmitter querySse(@RequestParam String keywords) {
        // 超时 60 秒：覆盖 Dify 检索耗时，超时由容器回调 onTimeout 结束流
        SseEmitter emitter = new SseEmitter(60_000L);
        SSE_EXECUTOR.execute(() -> {
            try {
                KnowledgeResponse response = knowledgeDatasetService.query(keywords);
                for (KnowledgeResponse.KnowledgeItem item : response.getItems()) {
                    emitter.send(SseEmitter.event().name("item")
                            .data(item, MediaType.APPLICATION_JSON));
                }
                emitter.send(SseEmitter.event().name("done")
                        .data("命中 " + response.getItems().size() + " 条", MediaType.TEXT_PLAIN));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE 检索推送失败: keywords={}", keywords, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("检索失败: " + e.getMessage(), MediaType.TEXT_PLAIN));
                } catch (Exception ignored) {
                    // 客户端已断开，无法再发送
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * 上传文本文档（运维用，仅教师）
     * <p>
     * POST /api/knowledge/dataset/upload/text
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/upload/text")
    public Result<UploadResponse> uploadText(@Valid @RequestBody UploadTextRequest request) {
        return Result.success(knowledgeDatasetService.uploadText(request));
    }

    /**
     * 上传文件文档（运维用，仅教师；文件类型需在 Dify 支持白名单内）
     * <p>
     * POST /api/knowledge/dataset/upload/file?chapterId=3，form-data: file
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/upload/file")
    public Result<UploadResponse> uploadFile(@RequestParam Long chapterId,
                                             @RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeDatasetService.uploadFile(chapterId, file));
    }

    /**
     * 更新文档元数据（运维用，仅教师）
     * <p>
     * POST /api/knowledge/dataset/metadata/update
     */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/metadata/update")
    public Result<String> updateMetadata(@Valid @RequestBody UpdateMetadataRequest request) {
        return Result.success(knowledgeDatasetService.updateMetadata(request));
    }
}
