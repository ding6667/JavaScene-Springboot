package com.amlve.system.service;

import com.amlve.system.dto.request.ClassSaveRequest;
import com.amlve.system.dto.response.ClassListResponse;
import com.amlve.system.dto.response.StudentListItemResponse;

import java.util.List;

/**
 * 班级维护。学生归属以 sys_student_info.class_id 为准，本服务不存学生名单
 */
public interface ClassService {

    /**
     * 班级列表，可按课程筛选。
     * <p>
     * 返回范围按当前登录者的数据权限收敛，这跟「角色能不能调这个接口」是两回事：
     * ADMIN 全量；TEACHER 仅自己任课的班；STUDENT 仅自己所在的那一个班
     */
    List<ClassListResponse> list(Long courseId);

    /**
     * 班级学生列表（仅用户信息，学习进度不在本任务范围）。教师只能查自己带的班
     */
    List<StudentListItemResponse> students(Long classId);

    /**
     * 新增班级
     *
     * @return 新班级ID
     */
    Long create(ClassSaveRequest request);

    /**
     * 更新班级。id 来自路径，不接受 body 里再传一份，避免路径与 body 指向不同对象
     */
    void update(Long id, ClassSaveRequest request);

    void delete(Long id);
}
