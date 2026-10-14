package com.amlve.study.service;

import com.amlve.study.dto.request.PracticeDraftRequest;
import com.amlve.study.dto.request.PracticeSubmitRequest;
import com.amlve.study.dto.response.PracticeDetailResponse;
import com.amlve.study.dto.response.PracticeDraftResponse;
import com.amlve.study.dto.response.PracticeSubmitResponse;

/**
 * 学生自主刷题服务（任务书 3.2 ~ 3.4）
 */
public interface PracticeService {

    /**
     * 正式提交答案：判题 → 调调试智能体 → 写运行记录 → upsert 练习记录与题目状态。
     *
     * <p>practice 一题只有一条（uk_user_question 幂等），run 一次运行一条；
     * 判题或 AI 失败时练习记录仍落库（status=1、结果字段置空并记 warn）。
     */
    PracticeSubmitResponse submit(PracticeSubmitRequest request, Long userId);

    /**
     * 保存草稿：不判题、不调智能体、不产生运行记录；同一题只保留最新草稿内容。
     */
    PracticeDraftResponse draft(PracticeDraftRequest request, Long userId);

    /**
     * 查询某题作答详情：无记录时返回空结构（不报 404，前端按「未作答」渲染）。
     */
    PracticeDetailResponse detail(Long userId, Long questionId);
}
