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
     * <p>出错（编译错误/运行时错误）时自动调用代码调试智能体，
     * 把错误原因、优化建议等回填到结果中。
     *
     * @param userId  当前登录学生ID（来自 Token，由 Controller 传入）
     * @param request 代码内容 + 关联章节/题目
     * @return 编译运行结果
     */
    CodeRunResultResponse run(Long userId, CodeRunRequest request);

    /**
     * 调试模式运行：编译运行后无论成败都调用代码调试智能体，
     * 运行成功时让智能体基于输出做知识点讲解，失败时返回调试建议。
     *
     * @param userId  当前登录学生ID（来自 Token，由 Controller 传入）
     * @param request 代码内容 + 关联章节/题目
     * @return 编译运行结果（含 AI 调试字段）
     */
    CodeRunResultResponse debugRun(Long userId, CodeRunRequest request);
}
