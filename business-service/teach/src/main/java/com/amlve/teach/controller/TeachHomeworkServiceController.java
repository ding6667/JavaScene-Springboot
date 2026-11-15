package com.amlve.teach.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.teach.service.impl.TeachServiceImpl;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkQuestionSnapshotDTO;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import com.amlve.teachapi.service.TeachHomeworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * TeachHomeworkService 的 HTTP 提供端：Feign 客户端按同一接口调用，方法映射继承自接口。
 * <p>
 * 委托给 {@link TeachServiceImpl}（该类同时实现模块内部的 TeachService 与本契约）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/teach/homework")
public class TeachHomeworkServiceController implements TeachHomeworkService {

    private final TeachServiceImpl teachHomeworkService;

    @Override
    public Result<HomeworkResponse> getHomeworkById(Long homeworkId) {
        return teachHomeworkService.getHomeworkById(homeworkId);
    }

    @Override
    public Result<List<HomeworkResponse>> listHomeworkByTeacherId(Long teacherId) {
        return teachHomeworkService.listHomeworkByTeacherId(teacherId);
    }

    @Override
    public Result<List<StudentHomeworkBriefResponse>> listStudentHomework(StudentHomeworkQueryRequest query) {
        return teachHomeworkService.listStudentHomework(query);
    }

    @Override
    public Result<PageResult<StudentHomeworkBriefResponse>> pageStudentHomework(StudentHomeworkQueryRequest query) {
        return teachHomeworkService.pageStudentHomework(query);
    }

    @Override
    public Result<StudentHomeworkDetailResponse> getStudentHomeworkDetail(Long homeworkId) {
        return teachHomeworkService.getStudentHomeworkDetail(homeworkId);
    }

    @Override
    public Result<HomeworkSubmitCheckResponse> checkHomeworkSubmit(Long homeworkId) {
        return teachHomeworkService.checkHomeworkSubmit(homeworkId);
    }

    @Override
    public Result<List<HomeworkQuestionSnapshotDTO>> getQuestionSnapshots(List<Long> snapshotIds) {
        return teachHomeworkService.getQuestionSnapshots(snapshotIds);
    }

    @Override
    public Result<Void> notifySubmit(Long homeworkId) {
        return teachHomeworkService.notifySubmit(homeworkId);
    }
}
