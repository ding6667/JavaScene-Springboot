package com.amlve.teach.service;

import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.request.TeachingMaterialDTO;

/**
 * 备课材料后台生成业务接口：调用智能体 → 生成 PPT → 回写已建好的备课记录。
 * <p>
 * 与同步受理接口 {@link TeachPrepareService} 分离，是因为异步任务必须由独立 Bean 承接
 * （同类自调用不走代理，{@code @Async} 会退化成同步执行）。
 */
public interface TeachPrepareAsyncService {

    /**
     * 后台生成备课材料并回写记录：成功置 status=1，失败置 status=0 并写 error_msg。
     * <p>
     * 异步线程没有 SecurityContext，教师ID 必须由请求线程取好后传入。
     *
     * @param recordId     备课记录ID（受理时已落库，status=2 生成中）
     * @param chapterId    章节ID（仅用于日志与 teach_ppt 落库）
     * @param teacherId    备课教师ID
     * @param agentRequest 智能体请求（模板骨架已填充）
     * @param material     章节备课素材
     * @param themeCode    PPT 模板主题码，无可用模板时为 null
     */
    void generateAsync(Long recordId, Long chapterId, Long teacherId,
                       PrepareRequest agentRequest, TeachingMaterialDTO material, String themeCode);
}
