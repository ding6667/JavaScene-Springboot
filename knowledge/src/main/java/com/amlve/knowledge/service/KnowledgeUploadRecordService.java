package com.amlve.knowledge.service;

import com.amlve.agentqaapi.dto.response.DifyDocumentVO;
import com.amlve.common.request.PageResult;
import com.amlve.knowledge.dto.request.UploadRecordQueryRequest;
import com.amlve.knowledge.dto.response.KnowledgeUploadRecordVO;
import com.amlve.knowledge.tableEnt.KnowledgeUploadRecord;

/**
 * 知识库上传留痕服务
 * <p>
 * 三个写方法都跑在独立事务（REQUIRES_NEW）里：上传失败时外层事务会回滚，
 * 若留痕同处一个事务就会一起被抹掉，"每次上传都留痕"就落空了。
 */
public interface KnowledgeUploadRecordService {

    /**
     * 发起上传时创建"处理中"记录
     *
     * @param datasetId   知识库ID
     * @param chapterId   章节ID
     * @param chapterName 章节名称（冗余）
     * @param fileName    原始文件名或文档名
     * @param fileSize    文件大小（字节），文本上传传 null
     * @param fileType    文件类型（扩展名）
     * @param uploadType  上传类型：1=新增，2=更新
     * @return 已落库的记录（含 id，供后续回写）
     */
    KnowledgeUploadRecord start(String datasetId, Long chapterId, String chapterName,
                                String fileName, Long fileSize, String fileType, Integer uploadType);

    /**
     * 回写成功状态
     *
     * @param record     记录（为 null 时忽略）
     * @param document   Dify 建档结果（含 documentId / batch / 分段数）
     * @param uploadType 实际上传类型：命中已有文档为"更新"，否则为"新增"
     */
    void markSuccess(KnowledgeUploadRecord record, DifyDocumentVO document, Integer uploadType);

    /**
     * 回写失败状态与原因。
     * <p>
     * 本方法绝不向外抛异常：留痕是旁路写入，若它自己出错还往上抛，
     * 会把真正的失败原因（Dify 报错）顶掉，前端只看到一条 SQL 异常。
     *
     * @param record     记录（可能尚未创建成功，传 null 时忽略）
     * @param documentId 已拿到的 Dify 文档ID（可为 null），失败时更需要它来核对残留
     * @param batch      批次标识（可为 null）
     * @param errorMsg   失败原因
     */
    void markFailed(KnowledgeUploadRecord record, String documentId, String batch, String errorMsg);

    /**
     * 分页查询上传记录（含数据隔离：教师只看本人）
     *
     * @param request 查询条件
     * @return 分页结果
     */
    PageResult<KnowledgeUploadRecordVO> listRecords(UploadRecordQueryRequest request);
}
