package com.amlve.teachapi;

import com.amlve.teachapi.dto.response.HomeworkResponse;

import java.util.List;

public interface TeachService {

    /**
     * 获取某章节课下作业（exam模块调用，用于学生提交）
     */
    List<HomeworkResponse> getByChapter(Long chapterId);

}
