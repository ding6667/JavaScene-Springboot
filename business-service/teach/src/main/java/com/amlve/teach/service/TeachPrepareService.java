package com.amlve.teach.service;

import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;

/**
 * 备课智能体业务接口：校验模板与素材 → 落"生成中"备课记录 → 交后台线程生成 → 秒回记录ID
 */
public interface TeachPrepareService {

    /**
     * 受理备课请求。
     * <p>
     * 只在受理线程内完成模板校验、章节素材查询和备课记录落库（status=2），随后把耗时的
     * 智能体调用与 PPT 转换交给 {@link TeachPrepareAsyncService}，因此返回体不含五类材料。
     * 前端凭返回的 recordId 轮询 {@code GET /api/teach/prepare/record/detail?id=} 取结果。
     *
     * @param request 备课请求（章节ID、五个生成开关、用户额外指令）
     * @return 受理结果（备课记录ID + status=2 生成中）
     * @throws com.amlve.common.exception.BusinessException 模板不可用或章节素材查询失败时抛出，
     *                                                     此时不会留下备课记录
     */
    PrepareResponse generateTeachingMaterials(PrepareRequest request);
}
