package com.amlve.study.service;

import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRunResultResponse;

/**
 * 学生代码编译运行服务
 */
public interface CodeRunService {

    /**
     * 编译并运行学生提交的 Java 代码。
     *
     * @param userId  当前登录学生ID（来自 Token，由 Controller 传入）
     * @param request 代码内容 + 关联章节/题目
     * @return 编译运行结果
     */
    CodeRunResultResponse run(Long userId, CodeRunRequest request);
}
