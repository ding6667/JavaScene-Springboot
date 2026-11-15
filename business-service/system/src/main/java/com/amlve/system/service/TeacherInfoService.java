package com.amlve.system.service;

import com.amlve.system.dto.request.TeacherInfoUpdateRequest;
import com.amlve.system.dto.response.TeacherInfoResponse;

/**
 * 教师履历扩展信息
 */
public interface TeacherInfoService {

    TeacherInfoResponse getByUserId(Long userId);

    /**
     * 本人完善教师扩展信息：首次必须填工号，之后工号只读
     */
    void updateSelf(Long userId, TeacherInfoUpdateRequest request);

    void createEmpty(Long userId);

    boolean existsTeacherNo(String teacherNo);
}
