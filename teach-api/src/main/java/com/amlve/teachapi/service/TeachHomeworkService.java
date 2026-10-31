package com.amlve.teachapi.service;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkQuestionSnapshotDTO;
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
     * 学生作业列表（分页版）：在原列表基础上带回总条数，供"我的作业"分页使用。
     * <p>
     * status 为空时返回学生可见范围（已发布 + 已截止），传值则按该状态精确过滤。
     * 班级/年级范围与 {@link #listStudentHomework(StudentHomeworkQueryRequest)} 一致。
     *
     * @param query 查询条件（班级、年级、状态、章节、分页）
     * @return 分页结果，records 为作业简要信息（不含答案）
     */
    Result<PageResult<StudentHomeworkBriefResponse>> pageStudentHomework(StudentHomeworkQueryRequest query);

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
     * 按快照ID批量取作业题目的完整载荷（含标准答案、解析、测试用例、判题方式）。
     * <p>
     * 【exam 调】批改归档错题时，用它把「发布那一版题」完整固化进错题快照，
     * 使错题重做按当时的用例判题，而不是按题库最新内容。
     * <p>
     * 仅在服务端内部流转，返回内容不得直接下发学生端。
     *
     * @param snapshotIds 作业题目快照ID（teach_homework_question.id）
     * @return 快照载荷列表；入参为空返回空列表，不存在的ID自动跳过
     */
    Result<List<HomeworkQuestionSnapshotDTO>> getQuestionSnapshots(List<Long> snapshotIds);

    /**
     * 通知提交成功（更新提交人数）
     * 【exam 调】exam 提交成功后回调 teach，teach 更新提交人数
     */
    Result<Void> notifySubmit(Long homeworkId);
}
