package com.amlve.study.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.study.dto.request.StudyProgressSaveRequest;
import com.amlve.study.dto.response.StudyProgressResponse;
import com.amlve.study.mapper.StudyProgressMapper;
import com.amlve.study.service.StudyProgressService;
import com.amlve.study.tableEnt.StudyLearningProgress;
import com.amlve.studyapi.constant.BehaviorType;
import com.amlve.studyapi.dto.request.StudyBehaviorReportRequest;
import com.amlve.studyapi.service.StudyBehaviorApi;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习进度实现：保存幂等（一人一章一条，重复提交不产生重复记录，进度只增不减、时长累加），
 * 查询返回章节标题（来自 knowledge-api，非直查表）。
 */
@Slf4j
@Service
public class StudyProgressServiceImpl
        extends ServiceImpl<StudyProgressMapper, StudyLearningProgress>
        implements StudyProgressService {

    /** 跨模块只依赖 knowledge-api 接口 */
    private final KnowledgeChapterApi knowledgeChapterApi;

    /** 学习行为上报（异步落库，失败不影响主流程） */
    @Autowired(required = false)
    private StudyBehaviorApi behaviorApi;

    public StudyProgressServiceImpl(KnowledgeChapterApi knowledgeChapterApi) {
        this.knowledgeChapterApi = knowledgeChapterApi;
    }

    @Override
    public Long saveProgress(Long userId, StudyProgressSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        StudyLearningProgress exist = lambdaQuery()
                .eq(StudyLearningProgress::getUserId, userId)
                .eq(StudyLearningProgress::getChapterId, request.getChapterId())
                .one();

        int rate = request.getProgressRate() == null ? 0 : request.getProgressRate();
        Long progressId;
        int totalDuration;

        if (exist == null) {
            StudyLearningProgress entity = new StudyLearningProgress();
            entity.setUserId(userId);                 // 关键：只来自 token
            entity.setChapterId(request.getChapterId());
            entity.setProgressRate(rate);
            entity.setStudyDuration(request.getStudyDuration() == null ? 0 : request.getStudyDuration());
            entity.setIsCompleted(resolveCompleted(rate));
            entity.setLastStudyTime(now);
            save(entity);
            progressId = entity.getId();
            totalDuration = entity.getStudyDuration();
        } else {
            // 已存在：更新（时长累加，进度取本次上报值）
            exist.setProgressRate(rate);
            if (request.getStudyDuration() != null) {
                exist.setStudyDuration((exist.getStudyDuration() == null ? 0 : exist.getStudyDuration())
                        + request.getStudyDuration());
            }
            exist.setIsCompleted(resolveCompleted(rate));
            exist.setLastStudyTime(now);
            updateById(exist);
            progressId = exist.getId();
            totalDuration = exist.getStudyDuration() == null ? 0 : exist.getStudyDuration();
        }

        // 行为埋点（异步落库，为学情分析积累数据）
        reportProgressBehavior(userId, request, rate, totalDuration);
        return progressId;
    }

    /** 进度保存行为埋点（PROGRESS_SAVE），失败仅记日志 */
    private void reportProgressBehavior(Long userId, StudyProgressSaveRequest request,
                                        int rate, int totalDuration) {
        if (behaviorApi == null) {
            return;
        }
        try {
            StudyBehaviorReportRequest report = new StudyBehaviorReportRequest();
            report.setUserId(userId);
            report.setBehaviorType(BehaviorType.PROGRESS_SAVE);
            report.setChapterId(request.getChapterId());
            report.setDuration(request.getStudyDuration());
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("progressRate", rate);
            detail.put("totalDuration", totalDuration);
            detail.put("isCompleted", resolveCompleted(rate));
            report.setDetail(detail);
            behaviorApi.report(report);
        } catch (Exception e) {
            log.warn("进度保存行为上报失败（不影响主流程）: {}", e.getMessage());
        }
    }

    @Override
    public List<StudyProgressResponse> listProgressByUser(Long userId, Long courseId) {
        // 1. 该课程下的全部章节（已按 sortOrder 升序）
        Result<List<ChapterDTO>> chaptersResult = knowledgeChapterApi.listChaptersByCourse(courseId);
        if (chaptersResult == null || chaptersResult.getCode() != ErrorCodeConstant.SUCCESS) {
            throw new BusinessException(ErrorMessageConstant.CHAPTER_QUERY_FAILED
                    + (chaptersResult == null ? "接口返回为空" : chaptersResult.getMessage()));
        }
        List<ChapterDTO> chapters = chaptersResult.getData();
        if (chapters == null || chapters.isEmpty()) {
            return List.of();
        }
        List<Long> chapterIds = chapters.stream()
                .map(ChapterDTO::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        // 2. 当前用户在这些章节下的进度记录
        Map<Long, StudyLearningProgress> progressMap = new LinkedHashMap<>();
        if (!chapterIds.isEmpty()) {
            lambdaQuery()
                    .eq(StudyLearningProgress::getUserId, userId)
                    .in(StudyLearningProgress::getChapterId, chapterIds)
                    .list()
                    .forEach(p -> progressMap.put(p.getChapterId(), p));
        }

        // 3. 以章节为准合并，未学习的章节返回进度 0
        return chapters.stream()
                .map(chapter -> toResponse(chapter, progressMap.get(chapter.getId())))
                .toList();
    }

    private StudyProgressResponse toResponse(ChapterDTO chapter, StudyLearningProgress entity) {
        StudyProgressResponse resp = new StudyProgressResponse();
        resp.setChapterId(chapter.getId());
        resp.setChapterName(chapter.getTitle());
        if (entity == null) {
            resp.setProgressRate(0);
            resp.setStudyDuration(0);
            resp.setIsCompleted(0);
            return resp;
        }
        resp.setId(entity.getId());
        resp.setProgressRate(entity.getProgressRate());
        resp.setStudyDuration(entity.getStudyDuration());
        resp.setIsCompleted(entity.getIsCompleted());
        resp.setLastStudyTime(entity.getLastStudyTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /**
     * 是否完成由进度自动推导：进度达到 100% 即标记为已完成(1)，否则未完成(0)。
     */
    private Integer resolveCompleted(Integer rate) {
        return rate != null && rate >= 100 ? 1 : 0;
    }
}
