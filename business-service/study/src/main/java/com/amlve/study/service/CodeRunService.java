package com.amlve.study.service;

import com.amlve.common.request.PageResult;
import com.amlve.study.dto.request.CodeRecordQueryRequest;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRecordDetailVO;
import com.amlve.study.dto.response.CodeRecordItemVO;
import com.amlve.study.dto.response.CodeRunResultResponse;
import com.amlve.study.judge.CodeJudgeResult;
import com.amlve.study.judge.TestCase;

import java.util.List;

/**
 * 学生代码编译运行服务
 */
public interface CodeRunService {

    /**
     * 编译并运行学生提交的 Java 代码。
     *
     * <p>出错（编译错误/运行时错误/超时）时自动调用代码调试智能体，
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

    /**
     * 编程题判题：编译一次、逐用例运行并比对输出，
     * 无论成败都调用代码调试智能体（安全拦截/系统错误除外），
     * AI 完整丰富字段随判题结果返回（随 run_record 落库）。
     */
    CodeJudgeResult judge(CodeRunRequest codeRunRequest, List<TestCase> cases, Long currentUserId);

    /**
     * 本人代码运行记录分页查询（任务书 3.5）。
     *
     * <p>列表不返回完整代码内容，仅返回 id、章节、状态、判题结果、耗时等摘要；
     * 查询条件：当前用户 + 可选 chapterId / questionId，按创建时间倒序。
     */
    PageResult<CodeRecordItemVO> pageRecords(Long userId, CodeRecordQueryRequest request);

    /**
     * 单条运行记录详情（任务书 3.6）：返回完整记录（含代码、用例明细、AI 字段）。
     *
     * <p>只能查本人记录，越权访问抛 403。
     */
    CodeRecordDetailVO getRecordDetail(Long userId, Long recordId);
}
