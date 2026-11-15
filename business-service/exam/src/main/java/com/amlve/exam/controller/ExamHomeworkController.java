package com.amlve.exam.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.exam.dto.request.HomeworkSubmitRequest;
import com.amlve.exam.service.ExamHomeworkService;
import com.amlve.exam.util.ExamAuthSupport;
import com.amlve.examapi.dto.request.StudentHomeworkListQueryRequest;
import com.amlve.examapi.dto.response.StudentHomeworkItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生作业接口：我的作业列表与作业提交。
 * <p>
 * 类级 {@code @PreAuthorize("isAuthenticated()")} 要求登录，
 * 列表与提交都只作用于当前登录用户（学生），班级范围由后端按学生档案解析。
 */
@RestController
@RequestMapping("/api/exam/homework")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExamHomeworkController {

    private final ExamHomeworkService homeworkService;
    private final ExamAuthSupport examAuthSupport;

    /**
     * 我的作业列表（学生）：按班级 + 年级取已发布/已截止作业，附带本人提交进度与批改结果。
     *
     * @param request 查询条件：status(2已发布/3已截止，不传=两者)、chapterId、onlyNotExpired、pageNum、pageSize
     * @return 作业分页（不含答案）
     */
    @GetMapping("/my-list")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<PageResult<StudentHomeworkItemVO>> myList(StudentHomeworkListQueryRequest request) {
        examAuthSupport.requireLogin();
        return Result.success(homeworkService.listMyHomework(request));
    }

    /** 学生提交作业答案（要求已登录） */
    @PostMapping("/submit")
    public Result<Void> submit(@Valid @RequestBody HomeworkSubmitRequest request) {
        examAuthSupport.requireLogin();
        homeworkService.submit(request);
        return Result.success(null);
    }
}
