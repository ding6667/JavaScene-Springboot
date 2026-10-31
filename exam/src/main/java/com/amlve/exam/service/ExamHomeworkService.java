package com.amlve.exam.service;

import com.amlve.common.request.PageResult;
import com.amlve.exam.dto.request.HomeworkSubmitRequest;
import com.amlve.examapi.dto.request.StudentHomeworkListQueryRequest;
import com.amlve.examapi.dto.response.StudentHomeworkItemVO;

/**
 * 作业服务：学生提交与「我的作业」查询。
 */
public interface ExamHomeworkService {

    /**
     * 学生提交作业答案，并在事务提交后触发异步批改。
     *
     * @param request 作业提交请求
     */
    void submit(HomeworkSubmitRequest request);

    /**
     * 当前登录学生的作业列表（我的作业）。
     * <p>
     * 作业范围按学生档案的班级/年级解析（含该年级的全年级作业），
     * 每项附带该生的提交进度与批改结果；无班级信息时返回空分页。
     *
     * @param request 查询条件（状态、章节、是否未截止、分页）
     * @return 作业列表分页（不含答案）
     */
    PageResult<StudentHomeworkItemVO> listMyHomework(StudentHomeworkListQueryRequest request);
}
