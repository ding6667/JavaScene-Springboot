package com.amlve.study.service.impl;

import com.amlve.common.request.Result;
import com.amlve.study.mapper.StudyBehaviorMapper;
import com.amlve.study.tableEnt.StudyBehavior;
import com.amlve.studyapi.dto.request.StudyBehaviorReportRequest;
import com.amlve.studyapi.service.StudyBehaviorApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 学习行为上报实现：异步落库 study_behavior（任务书验收「行为上报异步落库」）。
 *
 * <p>使用独立守护线程池而非 @Async，避免依赖全局 @EnableAsync 配置；
 * 上报与落库失败均只记日志，绝不影响调用方主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyBehaviorApiImpl implements StudyBehaviorApi {

    private final StudyBehaviorMapper behaviorMapper;

    /** 落库线程池：守护线程 + 有界队列，积压时丢弃并记日志 */
    private static final ExecutorService BEHAVIOR_POOL =
            Executors.newSingleThreadExecutor(daemonThreadFactory("study-behavior"));

    @Override
    public Result<Void> report(StudyBehaviorReportRequest request) {
        if (request == null || request.getUserId() == null || request.getBehaviorType() == null) {
            log.warn("行为上报参数不完整，忽略: {}", request);
            return Result.success(null);
        }
        try {
            BEHAVIOR_POOL.execute(() -> {
                try {
                    behaviorMapper.insert(toEntity(request));
                } catch (Exception e) {
                    log.warn("学习行为落库失败（不影响主流程）: userId={}, type={}, error={}",
                            request.getUserId(), request.getBehaviorType().getCode(), e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("学习行为上报提交失败（不影响主流程）: {}", e.getMessage());
        }
        return Result.success(null);
    }

    private StudyBehavior toEntity(StudyBehaviorReportRequest request) {
        StudyBehavior entity = new StudyBehavior();
        entity.setUserId(request.getUserId());
        entity.setBehaviorType(request.getBehaviorType().getCode());
        entity.setChapterId(request.getChapterId());
        entity.setQuestionId(request.getQuestionId());
        entity.setDuration(request.getDuration());
        entity.setDetail(request.getDetail());
        return entity;
    }

    private static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger seq = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
