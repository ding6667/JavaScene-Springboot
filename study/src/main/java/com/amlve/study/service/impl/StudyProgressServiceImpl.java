package com.amlve.study.service.impl;

import com.amlve.knowledgeapi.dto.response.ChapterDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.study.dto.request.StudyProgressSaveRequest;
import com.amlve.study.dto.response.StudyProgressResponse;
import com.amlve.study.mapper.StudyProgressMapper;
import com.amlve.study.service.StudyProgressService;
import com.amlve.study.tableEnt.StudyLearningProgress;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudyProgressServiceImpl
        extends ServiceImpl<StudyProgressMapper, StudyLearningProgress>
        implements StudyProgressService {

    /** 跨模块只依赖 knowledge-api 接口 */
    private final KnowledgeChapterApi knowledgeChapterApi;

    @Override
    public Long saveProgress(Long userId, StudyProgressSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        StudyLearningProgress exist = lambdaQuery()
                .eq(StudyLearningProgress::getUserId, userId)
                .eq(StudyLearningProgress::getChapterId, request.getChapterId())
                .one();

        int rate = request.getProgressRate() == null ? 0 : request.getProgressRate();

        if (exist == null) {
            StudyLearningProgress entity = new StudyLearningProgress();
            entity.setUserId(userId);                 // 关键：只来自 token
            entity.setChapterId(request.getChapterId());
            entity.setProgressRate(rate);
            entity.setStudyDuration(request.getStudyDuration() == null ? 0 : request.getStudyDuration());
            entity.setIsCompleted(resolveCompleted(rate));
            entity.setLastStudyTime(now);
            save(entity);
            return entity.getId();
        }

        // 已存在：更新
        exist.setProgressRate(rate);
        if (request.getStudyDuration() != null) {
            exist.setStudyDuration((exist.getStudyDuration() == null ? 0 : exist.getStudyDuration())
                    + request.getStudyDuration());
        }
        exist.setIsCompleted(resolveCompleted(rate));
        exist.setLastStudyTime(now);
        updateById(exist);
        return exist.getId();
    }

    @Override
    public List<StudyProgressResponse> listProgressByUser(Long userId, Long courseId) {
        // 1. 该课程下的全部章节（已按 sortOrder 升序）
        // TODO 需处理result.code == error 异常情况
        List<ChapterDTO> chapters = knowledgeChapterApi.listChaptersByCourse(courseId).getData();
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
                .map(chapter -> toResponse(chapter.getId(), progressMap.get(chapter.getId())))
                .toList();
    }

    private StudyProgressResponse toResponse(Long chapterId, StudyLearningProgress entity) {
        StudyProgressResponse resp = new StudyProgressResponse();
        resp.setChapterId(chapterId);
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
