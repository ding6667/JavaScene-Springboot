package com.amlve.agentqa.controller;

import com.amlve.agentqa.client.QAAgentClientImpl;
import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.agentqaapi.dto.response.QaWorkflowAnswerVO;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * QAAgentClient 的 HTTP 提供端：路径与参数映射同 Feign 接口一致。
 * <p>
 * 不直接 implements QAAgentClient：文件上传在 HTTP 侧以 MultipartFile 接收，
 * 落盘保留原始文件名后转成 File 交给本地实现（Dify 以上传文件名作知识库文档名）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/agent/qa")
public class QAAgentApiController {

    private final QAAgentClientImpl qaAgentApi;

    @PostMapping("/createDocumentByText")
    public Result<DifyDocumentVO> createDocumentByText(@RequestParam("datasetId") String datasetId,
                                                       @RequestParam("documentName") String documentName,
                                                       @RequestParam("text") String text) {
        return qaAgentApi.createDocumentByText(datasetId, documentName, text);
    }

    @PostMapping("/updateDocumentMetadata")
    public Result<String> updateDocumentMetadata(@RequestParam("datasetId") String datasetId,
                                                 @RequestParam("documentId") String documentId,
                                                 @RequestBody Map<String, Object> metadata) {
        return qaAgentApi.updateDocumentMetadata(datasetId, documentId, metadata);
    }

    @PostMapping(value = "/createDocumentByFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DifyDocumentVO> createDocumentByFile(@RequestParam("datasetId") String datasetId,
                                                       @RequestPart("file") MultipartFile file,
                                                       @RequestParam("documentName") String documentName,
                                                       @RequestPart("metadata") Map<String, Object> metadata) {
        Path tempDir = null;
        File tempFile = null;
        try {
            tempDir = Files.createTempDirectory("qa_upload_");
            String originalName = file.getOriginalFilename();
            String safeName = (originalName == null || originalName.isBlank()) ? "document" : originalName;
            tempFile = tempDir.resolve(safeName).toFile();
            file.transferTo(tempFile);
            return qaAgentApi.createDocumentByFile(datasetId, tempFile, documentName, metadata);
        } catch (IOException e) {
            throw new BusinessException("文件暂存失败: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
            }
            if (tempDir != null && tempDir.toFile().exists() && !tempDir.toFile().delete()) {
                log.warn("临时目录删除失败: {}", tempDir);
            }
        }
    }

    @PostMapping("/createChapterDocument")
    public Result<Void> createChapterDocument(@RequestParam("datasetId") String datasetId,
                                              @RequestParam("documentName") String documentName,
                                              @RequestParam("chapterContent") String chapterContent,
                                              @RequestParam("chapterId") Integer chapterId) {
        return qaAgentApi.createChapterDocument(datasetId, documentName, chapterContent, chapterId);
    }

    @PostMapping("/deleteDocument")
    public Result<Void> deleteDocument(@RequestParam("datasetId") String datasetId,
                                       @RequestParam("documentId") String documentId) {
        return qaAgentApi.deleteDocument(datasetId, documentId);
    }

    @PostMapping("/retrieve")
    public Result<DifyRetrieveResponse> retrieve(@RequestParam("datasetId") String datasetId,
                                                 @RequestParam("query") String query,
                                                 @RequestParam(value = "topK", required = false) Integer topK,
                                                 @RequestParam(value = "scoreThreshold", required = false) Double scoreThreshold) {
        return qaAgentApi.retrieve(datasetId, query, topK, scoreThreshold);
    }

    @PostMapping("/answer")
    public Result<QaWorkflowAnswerVO> answer(@RequestParam("chapterId") String chapterId,
                                             @RequestParam("userQuery") String userQuery) {
        return qaAgentApi.answer(chapterId, userQuery);
    }
}