package com.amlve.agentqaapi.service;


import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;
import com.amlve.agentqaapi.dto.response.QaWorkflowAnswerVO;
import com.amlve.common.request.Result;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Map;

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
    Result<DifyDocumentVO> createDocumentByText(String datasetId, String documentName, String text );

    /**
     * 更新元数据
     * POST /datasets/{dataset_id}/documents/metadata
     * @param datasetId
     * @param documentId
     * @param metadata
     * @return
     */
    Result<String> updateDocumentMetadata(String datasetId, String documentId, Map<String, Object> metadata);

    /**
     * 内部调用createDocumentByText和updateDocumentMetadata
     *
     * @param datasetId
     * @param documentName
     * @param chapterContent
     * @param chapterId
     */
    Result<Void> createChapterDocument(String datasetId, String documentName, String chapterContent, Integer chapterId);

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
    Result<DifyDocumentVO> createDocumentByFile(String datasetId, File file, String documentName, Map<String, Object> metadata);

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
    Result<Void> deleteDocument(String datasetId, String documentId);

    /**
     * 根据关键词从 Dify 知识库检索相关文档片段（自定义参数）
     *
     * @param datasetId      知识库ID
     * @param query          检索关键词
     * @param topK           返回条数
     * @param scoreThreshold 相似度阈值
     * @return Dify 检索响应
     */
    Result<DifyRetrieveResponse> retrieve(String datasetId, String query,
                                  Integer topK, Double scoreThreshold);

    /**
     * 答疑工作流：学生针对某章节提问，由 Dify 工作流结合该章节教材内容回答
     * POST /workflows/run（blocking 模式）
     *
     * @param chapterId 章节ID（工作流 inputs 变量名需与 Dify 工作流编排定义一致）
     * @param userQuery 学生提问内容（多轮上下文由调用方拼进这段文本）
     * @return 答案与本轮执行元数据（Dify 执行标识、token、耗时），供问答记录落库
     */
    Result<QaWorkflowAnswerVO> answer(String chapterId, String userQuery);

    /**
     * 答疑工作流（流式）：与 {@link #answer} 入参语义一致，
     * 但以 SSE 流式返回文本片段，前端可边生成边展示（对应测试报告 S-08）。
     * <p>
     * 流式语义与 Result 包装不兼容，故直接返回 Flux：
     * 出错时以 Flux 错误信号终止，由控制层转为 error 事件。
     * <p>
     * 与 blocking 的 {@link #answer} 不同：本方法<b>不剥除</b>模型思考链，
     * 按 Dify 片段原样返回，think 与正文的分流由控制层负责。
     *
     * @param chapterId 章节ID（可为 null / 空串，表示不限章节）
     * @param userQuery 学生提问内容
     * @return 文本片段流（含思考链标签原文），流自然结束表示回答完成
     */
    Flux<String> answerStream(String chapterId, String userQuery);
}
