package com.amlve.study.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.study.dto.request.PracticeDraftRequest;
import com.amlve.study.dto.request.PracticeSubmitRequest;
import com.amlve.study.dto.response.PracticeDetailResponse;
import com.amlve.study.dto.response.PracticeDraftResponse;
import com.amlve.study.dto.response.PracticeSubmitResponse;
import com.amlve.study.service.PracticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生自主刷题（任务书 3.1 ~ 3.4）：
 * 提交 / 草稿 / 详情，userId 一律取自 Token（SecurityUtils），禁止传他人 ID。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study/practice")
@PreAuthorize("hasRole('STUDENT')")
public class PracticeController {

    private final PracticeService practiceService;

    /**
     * 正式提交答案：判题 → 调调试智能体 → 写运行记录 → upsert 练习记录与题目状态
     */
    @PostMapping("/submit")
    public Result<PracticeSubmitResponse> submitPractice(@Valid @RequestBody PracticeSubmitRequest request) {
        Long userId = SecurityUtils.getUserId();
        log.info("学生提交刷题: userId={}, questionId={}", userId, request.getQuestionId());
        return Result.success(practiceService.submit(request, userId));
    }

    /**
     * 保存草稿：不判题、不调智能体、不产生运行记录；接口幂等，前端自行节流
     */
    @PostMapping("/draft")
    public Result<PracticeDraftResponse> saveDraft(@Valid @RequestBody PracticeDraftRequest request) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(practiceService.draft(request, userId));
    }

    /**
     * 查询某题作答详情（含最新一次运行结果摘要）；未作答返回空结构而非 404
     */
    @GetMapping("/detail")
    public Result<PracticeDetailResponse> getPracticeDetail(
            @RequestParam @NotNull(message = "题目ID不能为空") Long questionId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(practiceService.detail(userId, questionId));
    }
}
