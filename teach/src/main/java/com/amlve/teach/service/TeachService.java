package com.amlve.teach.service;

import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.dto.response.HomeworkResponse;
import com.amlve.teach.dto.response.StudentHomeworkBrief;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;

import java.util.List;

/**
 * 教师教学模块业务接口
 */
public interface TeachService {

    /**
     * 教师发布作业，返回新建作业的ID
     */
    Long publishHomework(HomeworkPublishRequest request);

    /**
     * 查询当前登录教师发布的作业列表
     */
    List<HomeworkResponse> listMyHomework();

    /**
     * 查询当前登录教师的作业详情（含题目完整快照），非本人作业拒绝访问
     */
    HomeworkDetailResponse getMyHomeworkDetail(Long homeworkId);

    /**
     * 关闭已发布的作业（截止收题），仅 PUBLISHED 状态可关闭
     */
    void closeHomework(HomeworkCloseRequest request);

    /**
     * 学生端：作业简短信息 + 题目快照ID列表。
     * <p>
     * 只返回作业元信息与 {@code teach_homework_question.id} 列表，不返回题干与答案；
     * 前端拿 id 列表后逐题调 {@link #getStudentHomeworkQuestion(Long)} 懒加载。
     *
     * @param homeworkId 作业ID
     * @return 简短信息（questionIds 按 sortOrder 升序）
     * @throws com.amlve.common.exception.BusinessException 作业不存在或学生不可见时抛出
     */
    StudentHomeworkBrief getStudentHomeworkBrief(Long homeworkId);

    /**
     * 学生端：按题目快照ID查单题详情（题干、选项、分值、渲染配置），不含答案与解析。
     *
     * @param questionId 作业题目ID（teach_homework_question.id，不是题库题目ID）
     * @return 学生端题目视图
     * @throws com.amlve.common.exception.BusinessException 题目不存在或所属作业学生不可见时抛出
     */
    StudentHomeworkDetailResponse.StudentQuestionItem getStudentHomeworkQuestion(Long questionId);
}
