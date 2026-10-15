package com.amlve.teach.service;

import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.dto.response.HomeworkResponse;

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
}
