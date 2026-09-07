package com.amlve.teach.service;

import com.amlve.teach.dto.HomeworkPublishRequest;
import com.amlve.teach.dto.HomeworkResponse;

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
}
