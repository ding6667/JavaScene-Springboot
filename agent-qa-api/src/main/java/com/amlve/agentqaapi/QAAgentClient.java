package com.amlve.agentqaapi;


import com.amlve.agentqaapi.dto.response.DifyRetrieveResponse;

import java.io.File;
import java.util.Map;

public interface QAAgentClient {
    /**
     * 纯文本创建文档 不附带元数据
     * @param datasetId
     * @param documentName
     * @param text
     * @return
     */
    String createDocumentByText(String datasetId, String documentName, String text );

    /**
     * 更新元数据
     * POST /datasets/{dataset_id}/documents/metadata
     * @param datasetId
     * @param documentId
     * @param metadata
     * @return
     */
    String updateDocumentMetadata(String datasetId, String documentId, Map<String, Object> metadata);

    /**
     * 内部调用createDocumentByText和updateDocumentMetadata
     *
     * @param datasetId
     * @param documentName
     * @param chapterContent
     * @param chapterId
     */
    void createChapterDocument(String datasetId, String documentName, String chapterContent, Integer chapterId);

    /**
     * 通过上传文件创建文档
     * POST /datasets/{dataset_id}/document/create-by-file
     *
     * @param datasetId 知识库ID
     * @param file 要上传的文件
     * @param documentName 文档名称（可选，不传则使用文件名）
     * @param metadata 自定义元数据（如 chapter_id, chapter_name）
     * @return 返回的 document_id
     */
    String createDocumentByFile(String datasetId, File file, String documentName, Map<String, Object> metadata);

    /**
     * 根据关键词从 Dify 知识库检索相关文档片段（自定义参数）
     *
     * @param datasetId      知识库ID
     * @param query          检索关键词
     * @param topK           返回条数
     * @param scoreThreshold 相似度阈值
     * @return Dify 检索响应
     */
    DifyRetrieveResponse retrieve(String datasetId, String query,
                                  Integer topK, Double scoreThreshold);

    /**
     * 答疑工作流：学生针对某章节提问，由 Dify 工作流结合该章节教材内容回答
     * POST /workflows/run（blocking 模式）
     *
     * @param chapterId 章节ID（工作流 inputs 变量名需与 Dify 工作流编排定义一致）
     * @param userQuery 学生提问内容
     * @return 工作流输出中的答案文本
     */
    String answer(String chapterId, String userQuery);
}
