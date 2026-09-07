package com.amlve.teachapi;

import com.amlve.teachapi.dto.HomeworkDTO;

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
    HomeworkDTO getHomeworkById(Long homeworkId);

    /**
     * 查询某教师发布的作业列表
     *
     * @param teacherId 教师ID
     * @return 作业列表
     */
    List<HomeworkDTO> listHomeworkByTeacherId(Long teacherId);
}
