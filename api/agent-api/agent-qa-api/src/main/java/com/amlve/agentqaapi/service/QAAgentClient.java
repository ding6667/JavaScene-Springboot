package com.amlve.agentqaapi.service;


import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.agentqaapi.dto.response.QaWorkflowAnswerVO;
import com.amlve.common.request.Result;
import com.amlve.common.web.feign.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.File;
import java.util.Map;

/**
 * 答疑智能体跨服务 Feign 客户端。
 * <p>
 * 文件上传 createDocumentByFile 以 multipart 承载（feign-form 编码，File 保留原始文件名）；
 * SSE 流式应答 answerStream 不走 Feign，留在 agent-qa 本地实现类上由 Web 接口直接调用。
 */
@FeignClient(name = "agent-qa-service", path = "/internal/agent/qa", configuration = FeignConfig.class)
public interface QAAgentClient {
    /**
     * 纯文本创建文档 不附带元数据
     * <p>
     * 建档后会轮询 Dify 索引状态：completed 且分段数 &gt; 0 才算成功；
     * error / 空壳（分段为 0）/ 超时三种情况会先删除该异常文档再抛业务异常，
     * 避免"接口返回成功但知识库里没有内容"的假成功。
     *
     * @param datasetId
     * @param documentName
     * @param text
     * @return 文档ID + batch + 分段总数
     */
    @PostMapping("/createDocumentByText")
    Result<DifyDocumentVO> createDocumentByText(@RequestParam("datasetId") String datasetId, @RequestParam("documentName") String documentName, @RequestParam("text") String text );

    /**
     * 更新元数据
     * POST /datasets/{dataset_id}/documents/metadata
     * @param datasetId
     * @param documentId
     * @param metadata
     * @return
     */
    @PostMapping("/updateDocumentMetadata")
    Result<String> updateDocumentMetadata(@RequestParam("datasetId") String datasetId, @RequestParam("documentId") String documentId, @RequestBody Map<String, Object> metadata);

    /**
     * 通过上传文件创建文档
     * POST /datasets/{dataset_id}/document/create-by-file
     * <p>
     * 与 {@link #createDocumentByText} 同样带索引校验与失败清理。
     * 注意 Dify 以"上传文件名"作为知识库文档名（data 里的 documentName 会被忽略），
     * 因此传入的 File 必须保留原始文件名。
     *
     * @param datasetId 知识库ID
     * @param file 要上传的文件
     * @param documentName 文档名称（可选，不传则使用文件名）
     * @param metadata 自定义元数据（如 chapter_id, chapter_name）
     * @return 文档ID + batch + 分段总数
     */
    @PostMapping(value = "/createDocumentByFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<DifyDocumentVO> createDocumentByFile(@RequestParam("datasetId") String datasetId,
                                                @RequestPart("file") File file,
                                                @RequestParam("documentName") String documentName,
                                                @RequestPart("metadata") Map<String, Object> metadata);

    /**
     * 内部调用createDocumentByText和updateDocumentMetadata
     *
     * @param datasetId
     * @param documentName
     * @param chapterContent
     * @param chapterId
     */
    @PostMapping("/createChapterDocument")
    Result<Void> createChapterDocument(@RequestParam("datasetId") String datasetId, @RequestParam("documentName") String documentName, @RequestParam("chapterContent") String chapterContent, @RequestParam("chapterId") Integer chapterId);

    /**
     * 删除知识库文档
     * DELETE /datasets/{dataset_id}/documents/{document_id}
     * <p>
     * 幂等：文档已不存在（Dify 返回 404）时视为删除成功，不抛异常，
     * 以免清理动作反过来打断主流程。
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @return 成功标志
     */
    @PostMapping("/deleteDocument")
    Result<Void> deleteDocument(@RequestParam("datasetId") String datasetId, @RequestParam("documentId") String documentId);

    /**
     * 根据关键词从 Dify 知识库检索相关文档片段（自定义参数）
     *
     * @param datasetId      知识库ID
     * @param query          检索关键词
     * @param topK           返回条数
     * @param scoreThreshold 相似度阈值
     * @return Dify 检索响应
     */
    @PostMapping("/retrieve")
    Result<DifyRetrieveResponse> retrieve(@RequestParam("datasetId") String datasetId, @RequestParam("query") String query,
                                  @RequestParam("topK") Integer topK, @RequestParam("scoreThreshold") Double scoreThreshold);

    /**
     * 答疑工作流：学生针对某章节提问，由 Dify 工作流结合该章节教材内容回答
     * POST /workflows/run（blocking 模式）
     *
     * @param chapterId 章节ID（工作流 inputs 变量名需与 Dify 工作流编排定义一致）
     * @param userQuery 学生提问内容（多轮上下文由调用方拼进这段文本）
     * @return 答案与本轮执行元数据（Dify 执行标识、token、耗时），供问答记录落库
     */
    @PostMapping("/answer")
    Result<QaWorkflowAnswerVO> answer(@RequestParam("chapterId") String chapterId, @RequestParam("userQuery") String userQuery);
}
