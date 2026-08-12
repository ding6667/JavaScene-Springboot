package com.amlve.grading.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.UserContext;
import com.amlve.grading.service.GradingTaskService;
import com.amlve.grading.vo.GradingTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/grading/task")
@RequiredArgsConstructor
public class GradingTaskController {

    private final GradingTaskService gradingTaskService;

    @GetMapping("/zipList")
    public Result<PageResult<GradingTaskResponse>> zipList(@RequestParam(required = false) String semester,
                                                            @RequestParam(required = false) String taskCategory,
                                                            @RequestParam(required = false) Integer taskSequence,
                                                            @RequestParam(required = false) String classId,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "10") Integer size,
                                                            @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(gradingTaskService.pageZipTasks(UserContext.getUserId(), semester, taskCategory,
                taskSequence, classId, status, page, size, sortOrder));
    }

    @GetMapping("/fileList")
    public Result<PageResult<GradingTaskResponse>> fileList(@RequestParam(required = false) String semester,
                                                             @RequestParam(required = false) String taskCategory,
                                                             @RequestParam(required = false) Integer taskSequence,
                                                             @RequestParam(required = false) String studentId,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "10") Integer size,
                                                             @RequestParam(defaultValue = "desc") String sortOrder) {
        return Result.success(gradingTaskService.pageFileTasks(UserContext.getUserId(), semester, taskCategory,
                taskSequence, studentId, status, page, size, sortOrder));
    }

    @GetMapping("/detail/{taskId}")
    public Result<GradingTaskResponse> detail(@PathVariable String taskId) {
        return Result.success(gradingTaskService.getDetail(taskId));
    }
}