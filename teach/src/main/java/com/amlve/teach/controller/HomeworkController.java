package com.amlve.teach.controller;

import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.dto.response.HomeworkResponse;
import com.amlve.teach.dto.response.StudentHomeworkBrief;
import com.amlve.teach.service.TeachService;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 作业管理接口（教师端）
 */
@RestController
@RequestMapping("/api/teach/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final TeachService teachService;

    /** 教师发布作业 */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody HomeworkPublishRequest request) {
        return Result.success(teachService.publishHomework(request));
    }

    /** 教师查询自己发布的作业列表 */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/list")
    public Result<List<HomeworkResponse>> list() {
        return Result.success(teachService.listMyHomework());
    }

    /** 教师查询作业详情（含题目完整快照），非本人作业拒绝访问 */
    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/detail")
    public Result<HomeworkDetailResponse> detail(@RequestParam("homeworkId") Long homeworkId) {
        return Result.success(teachService.getMyHomeworkDetail(homeworkId));
    }

    /** 教师关闭已发布的作业（截止收题） */
    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/close")
    public Result<Void> close(@Valid @RequestBody HomeworkCloseRequest request) {
        teachService.closeHomework(request);
        return Result.success(null);
    }

    // ==================== 学生端接口 ====================

    /**
     * 学生端：作业简短信息 + 题目快照ID列表。
     * <p>
     * 只返回作业元信息与 {@code teach_homework_question.id} 列表（按 sortOrder 升序），
     * 不含题干/答案；前端拿 id 列表后逐题调 {@code /student/question} 懒加载。
     */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/brief")
    public Result<StudentHomeworkBrief> studentBrief(@RequestParam("homeworkId") Long homeworkId) {
        return Result.success(teachService.getStudentHomeworkBrief(homeworkId));
    }

    /**
     * 学生端：按题目快照ID查单题详情（题干、选项、分值、渲染配置），不含答案与解析。
     * <p>
     * {@code questionId} 用 {@code /student/brief} 返回的 {@code questionIds} 元素，
     * 即 {@code teach_homework_question.id}，不是题库题目ID。
     */
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/question")
    public Result<StudentHomeworkDetailResponse.StudentQuestionItem> studentQuestion(
            @RequestParam("questionId") Long questionId) {
        return Result.success(teachService.getStudentHomeworkQuestion(questionId));
    }
}
