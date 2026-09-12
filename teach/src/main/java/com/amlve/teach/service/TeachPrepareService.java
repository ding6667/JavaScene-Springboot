package com.amlve.teach.service;

import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;

/**
 * 备课智能体业务接口：查询素材 → 调用智能体 → 生成PPT → 落库 → 返回备课结果
 */
public interface TeachPrepareService {

    /**
     * 生成备课材料（教案、PPT、重难点、课后习题、上机实训）
     *
     * @param request 备课请求（章节ID、五个生成开关、用户额外指令）
     * @return 备课结果（含 PPT 文件路径与备课记录ID）
     */
    PrepareResponse generateTeachingMaterials(PrepareRequest request);
}
