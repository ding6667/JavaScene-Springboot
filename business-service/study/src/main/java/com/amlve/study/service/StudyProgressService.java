package com.amlve.study.service;

import com.amlve.study.dto.request.StudyProgressSaveRequest;
import com.amlve.study.dto.response.StudyProgressResponse;

import java.util.List;

public interface StudyProgressService {
    /** 保存/更新当前登录学生某章节的学习进度，返回记录 id */
    Long saveProgress(Long userId, StudyProgressSaveRequest request);

    /** 查询当前登录学生在指定课程下所有章节的学习进度（未学习章节进度为 0） */
    List<StudyProgressResponse> listProgressByUser(Long userId, Long courseId);
}
