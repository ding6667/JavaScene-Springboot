package com.amlve.teachapi.service;

import com.amlve.common.request.Result;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;

import java.util.List;

/**
 * teach 模块对外接口定义，供其他模块（如 exam 智能考评）跨模块调用。
 * 本接口由 teach 模块的 TeachServiceImpl 实现，调用方禁止直接注入实现类。
 */
public interface TeachHomeworkService {

    /**
     * 根据ID查询作业详情（exam模块批改时需要）
     *
     * @param homeworkId 作业ID
     * @return 作业信息，不存在时返回 null
     */
    Result<HomeworkResponse> getHomeworkById(Long homeworkId);

    /**
     * 查询某教师发布的作业列表
     *
     * @param teacherId 教师ID
     * @return 作业列表
     */
    Result<List<HomeworkResponse>> listHomeworkByTeacherId(Long teacherId);

    /**
     * 学生作业列表：按班级查已发布作业（去答案）
     * 【study 调】学生查看"我的作业"列表
     */
    Result<List<StudentHomeworkBriefResponse>> listStudentHomework(StudentHomeworkQueryRequest query);

    /**
     * 学生作业详情：作业元信息 + 题目快照（去答案）
     * 【study / exam 调】学生打开作业、exam 判题时拿题目快照
     */
    Result<StudentHomeworkDetailResponse> getStudentHomeworkDetail(Long homeworkId);

    /**
     * 校验作业是否可提交
     * 【exam 调】学生提交前，exam 校验作业状态、截止时间
     */
    Result<HomeworkSubmitCheckResponse> checkHomeworkSubmit(Long homeworkId);

    /**
     * 通知提交成功（更新提交人数）
     * 【exam 调】exam 提交成功后回调 teach，teach 更新提交人数
     */
    Result<Void> notifySubmit(Long homeworkId);
}
