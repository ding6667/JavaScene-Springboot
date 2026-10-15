package com.amlve.study.service.impl;

import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.agentcodeapi.service.CodeAgentClient;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.enums.CodeErrorTypeEnum;
import com.amlve.common.enums.JudgeResultEnum;
import com.amlve.common.enums.RunStatusEnum;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import com.amlve.knowledgeapi.dto.response.ChapterNameResponse;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.study.dto.request.CodeRecordQueryRequest;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRecordDetailVO;
import com.amlve.study.dto.response.CodeRecordItemVO;
import com.amlve.study.dto.response.CodeRunResultResponse;
import com.amlve.study.dto.response.TestResultVO;
import com.amlve.study.enumeration.CodeRunStatusEnum;
import com.amlve.study.judge.AnswerNormalizer;
import com.amlve.study.judge.CodeJudgeResult;
import com.amlve.study.judge.TestCase;
import com.amlve.study.mapper.StudyCodeRunRecordMapper;
import com.amlve.study.service.CodeRunService;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.amlve.studyapi.constant.BehaviorType;
import com.amlve.studyapi.dto.request.StudyBehaviorReportRequest;
import com.amlve.studyapi.service.StudyBehaviorApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 学生代码编译运行实现。
 *
 * <p>执行流程：
 * <ol>
 *   <li>静态黑名单扫描（先剥离注释/字符串字面量，避免绕过）</li>
 *   <li>解析主类名与 main 方法，提前拦截不规范代码</li>
 *   <li>在独立 UUID 临时目录写入源码</li>
 *   <li>用 JDK 内置 {@link JavaCompiler} 编译（限时 5s，禁用注解处理）</li>
 *   <li>用 {@link ProcessBuilder} 启动独立 JVM 进程运行（限时 5s，输出截断 4KB）</li>
 *   <li>无论成功失败都递归清理临时目录</li>
 * </ol>
 *
 * <p>状态口径（任务书 5.2 定稿）：响应 {@code status} 为力拓式字符串
 * AC/WA/TLE/MLE/RE/CE/OLE/SV/SE（权威源 judge_status 列）；
 * 旧 status 列（1-6）经 {@link CodeRunStatusEnum} 兼容映射保留。
 *
 * <p>AI 调试（任务书 2.1）：14 个入参全部由后端组装——章节名走 knowledge-api，
 * 题目内容/标准答案/要求走 exam-api，判题字段（test_result/passed_count/total_count）
 * 由运行与判题结果生成；AI 返回的完整丰富字段（codeQuality/complexity/steps 等）
 * 一并回填响应并落 JSON 列。
 */
@Slf4j
@Service
public class CodeRunServiceImpl implements CodeRunService {

    /**
     * 运行记录 Mapper。
     *
     * <p>使用非必需注入：单元测试直接 {@code new CodeRunServiceImpl()} 时不依赖 Spring 容器，
     * 此时该字段为 null，落库逻辑自动跳过，不影响编译运行主流程。
     */
    @Autowired(required = false)
    private StudyCodeRunRecordMapper codeRunRecordMapper;

    /**
     * 代码调试智能体客户端（agent-code-api 接口）。
     *
     * <p>与 Mapper 一致使用非必需注入：单元测试无 Spring 容器时为 null，
     * AI 调试逻辑自动跳过，不影响编译运行主流程。
     */
    @Autowired(required = false)
    private CodeAgentClient codeAgentClient;

    /**
     * 题目详情接口（exam-api）。用于调试智能体入参的题目内容/标准答案/要求，
     * 单测环境未装配时跳过补充，AI 入参按可选变量缺失处理。
     */
    @Autowired(required = false)
    private ExamQuestionApi examQuestionApi;

    /**
     * 章节接口（knowledge-api）。用于调试智能体入参的章节名称。
     */
    @Autowired(required = false)
    private KnowledgeChapterApi knowledgeChapterApi;

    /**
     * 学习行为上报（异步落库，失败不影响主流程）
     */
    @Autowired(required = false)
    private StudyBehaviorApi behaviorApi;

    /** AI 字段序列化（knowledgePoints 列表转 JSON 字符串落库） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 章节名称读取：knowledge-api 的 ChapterNameResponse 未提供 getter，
     * 通过字段可见的 Jackson 反射读取（不修改 knowledge-api 模块）。
     */
    private static final ObjectMapper FIELD_REFLECT_MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    // ===== 编译/运行参数 =====
    /** 编译超时时间（毫秒） */
    private static final long COMPILE_TIMEOUT_MS = 5000L;
    /** 运行超时时间（毫秒） */
    private static final long RUN_TIMEOUT_MS = 5000L;
    /** 单个流最大保留字符数，超出截断 */
    private static final int MAX_OUTPUT_CHARS = 4 * 1024;
    /** 子进程 JVM 堆上限 */
    private static final String MAX_HEAP_FLAG = "-Xmx64m";
    /** 子进程 JVM 线程栈大小 */
    private static final String STACK_SIZE_FLAG = "-Xss256k";

    // ===== 判题（跑测试用例）参数 =====
    /** 单题最多执行用例数，防止题目配置过多把请求拖死 */
    private static final int MAX_CASE_COUNT = 10;
    /** 单用例默认/最小/最大超时（毫秒） */
    private static final long DEFAULT_CASE_TIMEOUT_MS = 3000L;
    private static final long MIN_CASE_TIMEOUT_MS = 1000L;
    private static final long MAX_CASE_TIMEOUT_MS = 10000L;

    // ===== 临时工作区根目录 =====
    private static final Path TEMP_ROOT;

    static {
        TEMP_ROOT = Paths.get(System.getProperty("java.io.tmpdir"), "javascene-code-run");
        try {
            Files.createDirectories(TEMP_ROOT);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("无法创建代码运行临时目录: " + e.getMessage());
        }
    }

    /** 编译线程池：守护线程，避免拖住应用退出 */
    private static final ExecutorService COMPILE_POOL =
            Executors.newCachedThreadPool(daemonThreadFactory("code-compile"));

    /** 流读取线程池：标准输出/标准错误各占一个线程 */
    private static final ExecutorService STREAM_POOL =
            Executors.newCachedThreadPool(daemonThreadFactory("code-stream"));

    // ===== 代码结构识别 =====
    /** 主类名（public class Xxx） */
    private static final Pattern PUBLIC_CLASS_PATTERN =
            Pattern.compile("\\bpublic\\s+(?:final\\s+|abstract\\s+|strictfp\\s+)*class\\s+([A-Za-z_$][\\w$]*)");
    /** 兜底类名（class Xxx） */
    private static final Pattern ANY_CLASS_PATTERN =
            Pattern.compile("\\b(?:final\\s+|abstract\\s+|strictfp\\s+)*class\\s+([A-Za-z_$][\\w$]*)");
    /** 程序入口方法 */
    private static final Pattern MAIN_METHOD_PATTERN =
            Pattern.compile("\\b(?:public\\s+static|static\\s+public)\\s+void\\s+main\\s*\\(");

    // ===== 黑名单 API（静态扫描） =====
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("\\bRuntime\\s*\\.\\s*getRuntime\\s*\\("),
            Pattern.compile("\\bProcessBuilder\\b"),
            Pattern.compile("\\bnew\\s+Process\\b"),
            Pattern.compile("\\bnew\\s+File\\s*\\("),
            Pattern.compile("\\bnew\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter)\\b"),
            Pattern.compile("\\bnew\\s+(Socket|ServerSocket|DatagramSocket)\\s*\\("),
            Pattern.compile("\\bnew\\s+URL\\s*\\("),
            Pattern.compile("\\bURLConnection\\b|\\bHttpURLConnection\\b"),
            Pattern.compile("\\bClass\\s*\\.\\s*forName\\s*\\("),
            Pattern.compile("\\bgetDeclaredMethod\\s*\\(|\\bgetMethod\\s*\\(|\\bMethod\\s*\\.\\s*invoke\\s*\\("),
            Pattern.compile("\\bSystem\\s*\\.\\s*exit\\s*\\("),
            Pattern.compile("\\bSystem\\s*\\.\\s*setSecurityManager\\s*\\("),
            Pattern.compile("\\bjavax\\s*\\.\\s*script\\b|\\bScriptEngine\\b"),
            Pattern.compile("\\bFiles\\s*\\.\\s*(write|delete|create|copy|move|readAllBytes|readString)\\s*\\("),
            Pattern.compile("\\bjava\\s*\\.\\s*nio\\s*\\.\\s*file\\b")
    );

    /**
     * 异常堆栈中学生代码帧（形如 {@code at Main.main(Main.java:5)}），
     * 类名与文件名一致可排除 JDK 内部帧，用于运行时错误的本地行号定位。
     */
    private static final Pattern STACK_LINE_PATTERN =
            Pattern.compile("\\bat\\s+([\\w$]+)\\.[\\w$]+\\(\\1\\.java:(\\d+)\\)");

    @Override
    public CodeRunResultResponse run(Long userId, CodeRunRequest request) {
        CodeRunResultResponse response = doRun(userId, request);
        // 出错（编译/运行时错误/超时）时自动调用 AI 调试智能体，回填错误原因与优化建议
        invokeAiDebug(userId, request, response, false);
        // 每次运行都写入 study_code_run_record（含被拦截/失败的运行），落库异常不影响主流程
        saveRecord(userId, request, response);
        // 行为埋点（异步落库，为学情分析积累数据）
        reportBehavior(userId, request, response, BehaviorType.CODE_RUN);
        return response;
    }

    @Override
    public CodeRunResultResponse debugRun(Long userId, CodeRunRequest request) {
        CodeRunResultResponse response = doRun(userId, request);
        // 调试模式：无论成败都调用 AI 调试智能体（成功时 error_type=none 传运行输出）
        invokeAiDebug(userId, request, response, true);
        saveRecord(userId, request, response);
        reportBehavior(userId, request, response, BehaviorType.CODE_DEBUG);
        return response;
    }

    @Override
    public CodeJudgeResult judge(CodeRunRequest codeRunRequest, List<TestCase> cases, Long currentUserId) {
        long startMs = System.currentTimeMillis();
        CodeJudgeResult result = new CodeJudgeResult();

        if (cases == null || cases.isEmpty()) {
            finishJudge(result, RunStatusEnum.SYSTEM_ERROR, null,
                    "题目未配置测试用例，无法判分", null, List.of(), startMs);
            return result;
        }
        String code = codeRunRequest.getCodeContent();

        // 1. 安全扫描
        Pattern matched = staticBlacklistCheck(code);
        if (matched != null) {
            log.warn("判题代码安全扫描拒绝: userId={}, pattern={}", currentUserId, matched.pattern());
            finishJudge(result, RunStatusEnum.SECURITY_VIOLATION, null,
                    "代码中包含禁止使用的 API（" + matched.pattern() + "），请移除后重试", null, List.of(), startMs);
            return result;
        }

        // 2. 结构预检
        String stripped = stripCommentsAndStrings(code);
        String mainClass = detectMainClass(stripped);
        if (mainClass == null) {
            finishJudge(result, RunStatusEnum.COMPILE_ERROR, null,
                    "未找到类声明，请使用 public class Main 作为主类", null, List.of(), startMs);
            return result;
        }
        if (!MAIN_METHOD_PATTERN.matcher(stripped).find()) {
            finishJudge(result, RunStatusEnum.COMPILE_ERROR, null,
                    "未找到程序入口，请定义 public static void main(String[] args) 方法", null, List.of(), startMs);
            return result;
        }

        // 3. 独立临时目录
        Path workDir = TEMP_ROOT.resolve("judge_" + UUID.randomUUID().toString().replace("-", ""));
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            log.error("创建判题临时目录失败", e);
            finishJudge(result, RunStatusEnum.SYSTEM_ERROR, null,
                    "无法创建临时工作目录: " + e.getMessage(), null, List.of(), startMs);
            return result;
        }

        try {
            Path sourceFile = workDir.resolve(mainClass + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 4. 编译一次，后续用例复用 class 文件
            CompileOutcome compile = compile(sourceFile, workDir);
            if (!compile.success()) {
                finishJudge(result, RunStatusEnum.COMPILE_ERROR, null, compile.diagnostics(),
                        compile.errorLine(), List.of(), startMs);
                attachAi(codeRunRequest, result, currentUserId);
                return result;
            }

            // 5. 逐用例运行 + 输出比对
            List<TestCase> limited = cases.size() > MAX_CASE_COUNT ? cases.subList(0, MAX_CASE_COUNT) : cases;
            List<CodeJudgeResult.CaseResult> details = new ArrayList<>();
            RunStatusEnum status = RunStatusEnum.ACCEPTED;
            String summaryOutput = null;
            String summaryError = null;

            for (TestCase testCase : limited) {
                RunOutcome run = execute(workDir, mainClass,
                        testCase.input(), resolveCaseTimeout(testCase.timeLimitMs()));
                String actual = run.stdout();

                if (run.status() != RunStatusEnum.ACCEPTED) {
                    // 运行失败优先级高于答案错误：SE > RE > TLE
                    status = worseStatus(status, run.status() == RunStatusEnum.SYSTEM_ERROR
                            ? RunStatusEnum.SYSTEM_ERROR : run.status());
                    if (summaryError == null) {
                        summaryOutput = blankToNull(actual);
                        summaryError = "测试用例 #" + testCase.caseIndex()
                                + " 运行失败：" + blankToNull(run.stderr());
                    }
                    details.add(new CodeJudgeResult.CaseResult(testCase.caseIndex(), false,
                            testCase.input(), testCase.expectedOutput(),
                            blankToNull(actual), blankToNull(run.stderr()),
                            testCase.isSample(), testCase.caseType(), run.status()));
                    continue;
                }

                boolean ok = outputMatches(testCase.expectedOutput(), actual);
                if (ok) {
                    summaryOutput = blankToNull(actual);
                } else {
                    status = worseStatus(status, RunStatusEnum.WRONG_ANSWER);
                    if (summaryError == null) {
                        summaryOutput = blankToNull(actual);
                        summaryError = "测试用例 #" + testCase.caseIndex() + " 未通过：期望「"
                                + testCase.expectedOutput() + "」，实际「" + actual + "」";
                    }
                }
                details.add(new CodeJudgeResult.CaseResult(testCase.caseIndex(), ok,
                        testCase.input(), testCase.expectedOutput(),
                        blankToNull(actual), null,
                        testCase.isSample(), testCase.caseType(), RunStatusEnum.ACCEPTED));
            }

            finishJudge(result, status, summaryOutput, summaryError, null, details, startMs);
            // 刷题提交链路：判题后无论成败都调调试智能体（安全拦截/系统错误除外）
            attachAi(codeRunRequest, result, currentUserId);
            return result;

        } catch (IOException e) {
            log.error("判题 IO 异常: userId={}", currentUserId, e);
            finishJudge(result, RunStatusEnum.SYSTEM_ERROR, null,
                    "系统内部错误: " + e.getMessage(), null, List.of(), startMs);
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finishJudge(result, RunStatusEnum.SYSTEM_ERROR, null,
                    "系统内部错误: 线程被中断", null, List.of(), startMs);
            return result;
        } finally {
            cleanup(workDir);
        }
    }

    /** 汇总状态取更严重的一档：SE > RE > TLE > WA > AC */
    private RunStatusEnum worseStatus(RunStatusEnum current, RunStatusEnum incoming) {
        if (current == RunStatusEnum.SYSTEM_ERROR) {
            return current;
        }
        if (incoming == RunStatusEnum.SYSTEM_ERROR) {
            return incoming;
        }
        if (current == RunStatusEnum.RUNTIME_ERROR) {
            return current;
        }
        if (incoming == RunStatusEnum.RUNTIME_ERROR) {
            return incoming;
        }
        if (current == RunStatusEnum.TIME_LIMIT_EXCEEDED) {
            return current;
        }
        if (incoming == RunStatusEnum.TIME_LIMIT_EXCEEDED) {
            return incoming;
        }
        return incoming;
    }

    /** 输出比对：两侧同时归一化（multiline 严格比对 + compact 兜底「多行挤成一行」） */
    private boolean outputMatches(String expected, String actual) {
        String e = AnswerNormalizer.multiline(expected);
        String a = AnswerNormalizer.multiline(actual);
        return e.equals(a) || AnswerNormalizer.compact(e).equals(AnswerNormalizer.compact(a));
    }

    /** 用例超时：题目配置（毫秒）夹到 [1s, 10s]，未配置用默认 3s */
    private long resolveCaseTimeout(Integer timeLimitMs) {
        if (timeLimitMs == null || timeLimitMs <= 0) {
            return DEFAULT_CASE_TIMEOUT_MS;
        }
        return Math.min(Math.max(timeLimitMs, MIN_CASE_TIMEOUT_MS), MAX_CASE_TIMEOUT_MS);
    }

    private void finishJudge(CodeJudgeResult result, RunStatusEnum status, String output, String errorMsg,
                             Integer errorLine, List<CodeJudgeResult.CaseResult> details, long startMs) {
        result.setJudgeStatus(status);
        result.setJudgeResult(details.isEmpty()
                ? JudgeResultEnum.NOT_JUDGED
                : (details.stream().allMatch(CodeJudgeResult.CaseResult::passed)
                        ? JudgeResultEnum.ALL_PASSED : JudgeResultEnum.PARTIAL));
        result.setOutput(output);
        result.setErrorMsg(errorMsg);
        result.setErrorLine(errorLine);
        result.setCases(List.copyOf(details));
        result.setTotalCount(details.size());
        result.setPassedCount((int) details.stream().filter(CodeJudgeResult.CaseResult::passed).count());
        result.setRunDuration((int) Math.min(System.currentTimeMillis() - startMs, Integer.MAX_VALUE));
        result.setTestResult(toTestResultVO(result));
    }

    /** 判题明细转 TestResultVO（落库 test_result 列、透传给 AI 与前端） */
    private TestResultVO toTestResultVO(CodeJudgeResult result) {
        TestResultVO vo = new TestResultVO();
        vo.setPassedCount(result.getPassedCount());
        vo.setTotalCount(result.getTotalCount());
        if (result.getCases() != null) {
            vo.setCases(result.getCases().stream().map(c -> {
                TestResultVO.CaseVO item = new TestResultVO.CaseVO();
                item.setCaseIndex(c.caseIndex());
                item.setInput(c.input());
                item.setExpectedOutput(c.expectedOutput());
                item.setActualOutput(c.actualOutput());
                item.setPassed(c.passed());
                item.setIsSample(c.isSample());
                item.setCaseType(c.caseType());
                item.setError(c.errorMsg());
                return item;
            }).toList());
        }
        return vo;
    }

    /** 判题后调用 AI 调试（刷题提交链路约定：无论成败都调，安全拦截/系统错误除外） */
    private void attachAi(CodeRunRequest codeRunRequest, CodeJudgeResult result, Long currentUserId) {
        if (codeAgentClient == null) {
            return;
        }
        RunStatusEnum status = result.getJudgeStatus();
        if (status == RunStatusEnum.SECURITY_VIOLATION || status == RunStatusEnum.SYSTEM_ERROR) {
            return;
        }
        // 运行时错误本地从堆栈定位行号，回填判题结果，并作为 AI 调试参考
        if (result.getErrorLine() == null) {
            Integer extracted = extractErrorLineFromStack(result.getErrorMsg());
            if (extracted != null) {
                result.setErrorLine(extracted);
            }
        }
        String testResultJson = toJson(result.getTestResult());
        CodeDebugResponse debugResponse = callAgent(currentUserId, codeRunRequest, new AiContext(
                status, result.getOutput(), result.getErrorMsg(), result.getErrorLine(),
                testResultJson, result.getPassedCount(), result.getTotalCount()));
        if (debugResponse != null) {
            applyAiToJudgeResult(result, debugResponse);
        }
    }

    // ============ AI 调试智能体 ============

    /**
     * 调用代码调试智能体并回填 AI 字段。
     *
     * <p>触发规则：
     * <ul>
     *   <li>{@code always=true}（调试接口）：编译错误、运行时错误、运行成功都触发</li>
     *   <li>{@code always=false}（运行接口）：仅编译错误、运行时错误、超时触发</li>
     *   <li>安全拦截（SV）/系统错误（SE）：本地已给出明确原因，AI 无调试价值，不触发</li>
     * </ul>
     *
     * <p>智能体未装配（单测环境）或调用失败时仅记录日志，绝不影响编译运行主流程。
     */
    private void invokeAiDebug(Long userId, CodeRunRequest request,
                               CodeRunResultResponse response, boolean always) {
        if (codeAgentClient == null) {
            return;
        }
        RunStatusEnum status = RunStatusEnum.fromCode(response.getStatus());
        if (status == null || status == RunStatusEnum.SECURITY_VIOLATION
                || status == RunStatusEnum.SYSTEM_ERROR) {
            return;
        }
        boolean failed = status == RunStatusEnum.COMPILE_ERROR
                || status == RunStatusEnum.RUNTIME_ERROR
                || status == RunStatusEnum.TIME_LIMIT_EXCEEDED;
        if (!always && !failed) {
            return;
        }
        // 运行时错误本地从堆栈定位行号，回填响应（前端与落库可见），并作为 AI 调试参考
        if (response.getErrorLine() == null) {
            Integer extracted = extractErrorLineFromStack(response.getErrorMsg());
            if (extracted != null) {
                response.setErrorLine(extracted);
            }
        }
        CodeDebugResponse debugResponse = callAgent(userId, request, new AiContext(
                status, response.getOutput(), response.getErrorMsg(), response.getErrorLine(),
                null, null, null));
        if (debugResponse != null) {
            applyAiToResponse(response, debugResponse);
        }
    }

    /** 真正发起 AI 调试调用；任何异常只记日志，绝不影响判题/运行结果 */
    private CodeDebugResponse callAgent(Long userId, CodeRunRequest request, AiContext ctx) {
        try {
            CodeDebugRequest debugRequest = buildDebugRequest(request, ctx);
            Result<CodeDebugResponse> result = codeAgentClient.debug(debugRequest);
            if (result != null && result.getCode() == ErrorCodeConstant.SUCCESS) {
                return result.getData();
            }
            log.warn("AI 调试智能体返回失败: userId={}, message={}",
                    userId, result == null ? "接口返回为空" : result.getMessage());
        } catch (Exception e) {
            log.warn("AI 调试智能体调用失败，不影响运行结果返回: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    /** 运行/调试链路：AI 完整丰富字段一并回填响应（前端展示 + 落库数据源） */
    private void applyAiToResponse(CodeRunResultResponse response, CodeDebugResponse debugResponse) {
        response.setErrorLineList(debugResponse.getErrorLineList());
        response.setErrorReason(debugResponse.getErrorReason());
        response.setOptimizeSuggest(debugResponse.getOptimizeSuggest());
        response.setStandardIdea(debugResponse.getStandardIdea());
        response.setKnowledgePoints(debugResponse.getKnowledgePoints());
        response.setCodeQuality(debugResponse.getCodeQuality());
        response.setComplexity(debugResponse.getComplexity());
        response.setSteps(debugResponse.getSteps());
        response.setCommonMistakes(debugResponse.getCommonMistakes());
        response.setImprovements(debugResponse.getImprovements());
        response.setRelatedKnowledge(debugResponse.getRelatedKnowledge());
        response.setSimilarQuestions(debugResponse.getSimilarQuestions());
        response.setEncouragement(debugResponse.getEncouragement());
        response.setLearningSuggestion(debugResponse.getLearningSuggestion());
        // 本地未定位到错误行时，参考 AI 返回的行号列表
        if (response.getErrorLine() == null
                && debugResponse.getErrorLineList() != null
                && !debugResponse.getErrorLineList().isEmpty()) {
            try {
                response.setErrorLine(Integer.valueOf(debugResponse.getErrorLineList().get(0)));
            } catch (NumberFormatException ignore) {
                // AI 返回的行号不可解析时保留空值
            }
        }
    }

    /** 判题链路：AI 完整丰富字段回填判题结果（随 run_record 落库） */
    private void applyAiToJudgeResult(CodeJudgeResult result, CodeDebugResponse debugResponse) {
        result.setErrorLineList(debugResponse.getErrorLineList());
        result.setErrorReason(debugResponse.getErrorReason());
        result.setOptimizeSuggest(debugResponse.getOptimizeSuggest());
        result.setStandardIdea(debugResponse.getStandardIdea());
        result.setKnowledgePoints(debugResponse.getKnowledgePoints());
        result.setCodeQuality(debugResponse.getCodeQuality());
        result.setComplexity(debugResponse.getComplexity());
        result.setSteps(debugResponse.getSteps());
        result.setCommonMistakes(debugResponse.getCommonMistakes());
        result.setImprovements(debugResponse.getImprovements());
        result.setRelatedKnowledge(debugResponse.getRelatedKnowledge());
        result.setSimilarQuestions(debugResponse.getSimilarQuestions());
        result.setEncouragement(debugResponse.getEncouragement());
        result.setLearningSuggestion(debugResponse.getLearningSuggestion());
        if (result.getErrorLine() == null
                && debugResponse.getErrorLineList() != null
                && !debugResponse.getErrorLineList().isEmpty()) {
            try {
                result.setErrorLine(Integer.valueOf(debugResponse.getErrorLineList().get(0)));
            } catch (NumberFormatException ignore) {
                // AI 返回的行号不可解析时保留空值
            }
        }
    }

    /**
     * 把运行请求 + 运行/判题结果组装为调试智能体请求（任务书 2.1 的 14 个入参）。
     *
     * <p>章节名称走 knowledge-api，题目内容/标准答案/要求走 exam-api，
     * 接口未装配或查询失败时按可选变量缺失处理，不影响 AI 主流程。
     */
    private CodeDebugRequest buildDebugRequest(CodeRunRequest request, AiContext ctx) {
        CodeErrorTypeEnum errorType = resolveErrorType(ctx.status());
        Integer errorLine = ctx.errorLine();
        if (errorLine == null) {
            // 运行时错误本地从堆栈定位行号，回传 AI 的同时回填（前端与落库可见）
            errorLine = extractErrorLineFromStack(ctx.errorMsg());
        }
        CodeDebugRequest.CodeDebugRequestBuilder builder = CodeDebugRequest.builder()
                .code(request.getCodeContent())
                .chapterId(toStr(request.getChapterId()))
                .questionId(toStr(request.getQuestionId()))
                .errorType(errorType == null ? CodeErrorTypeEnum.NONE.getDifyValue() : errorType.getDifyValue())
                .errorMsg(ctx.errorMsg())
                .errorLine(errorLine == null ? null : String.valueOf(errorLine))
                .runResult(ctx.output());
        if (ctx.testResultJson() != null) {
            builder.testResult(ctx.testResultJson())
                    .passedCount(String.valueOf(ctx.passedCount()))
                    .totalCount(String.valueOf(ctx.totalCount()));
        }
        enrichChapterName(builder, request.getChapterId());
        enrichQuestion(builder, request.getQuestionId());
        return builder.build();
    }

    /** 运行状态 → Dify 错误类型 */
    private CodeErrorTypeEnum resolveErrorType(RunStatusEnum status) {
        if (status == null) {
            return CodeErrorTypeEnum.NONE;
        }
        return switch (status) {
            case COMPILE_ERROR -> CodeErrorTypeEnum.SYNTAX_ERROR;
            case RUNTIME_ERROR -> CodeErrorTypeEnum.RUNTIME_ERROR;
            case TIME_LIMIT_EXCEEDED -> CodeErrorTypeEnum.TIMEOUT;
            case MEMORY_LIMIT_EXCEEDED -> CodeErrorTypeEnum.MEMORY_OVERFLOW;
            case WRONG_ANSWER -> CodeErrorTypeEnum.LOGIC_ERROR;
            case SECURITY_VIOLATION -> CodeErrorTypeEnum.SECURITY_VIOLATION;
            default -> CodeErrorTypeEnum.NONE;
        };
    }

    /** 章节名称补充（knowledge-api，查询失败仅记日志） */
    private void enrichChapterName(CodeDebugRequest.CodeDebugRequestBuilder builder, Long chapterId) {
        if (knowledgeChapterApi == null || chapterId == null) {
            return;
        }
        try {
            Result<ChapterNameResponse> result = knowledgeChapterApi.getChapterNameById(chapterId);
            if (result != null && result.getCode() == ErrorCodeConstant.SUCCESS && result.getData() != null) {
                String name = readChapterName(result.getData());
                if (name != null && !name.isBlank()) {
                    builder.chapterName(name);
                }
            }
        } catch (Exception e) {
            log.warn("查询章节名称失败（AI 入参按缺失处理）: chapterId={}, error={}", chapterId, e.getMessage());
        }
    }

    /** ChapterNameResponse 未提供 getter，用字段可见的 Jackson 反射读取章节名 */
    private String readChapterName(ChapterNameResponse response) {
        try {
            JsonNode node = FIELD_REFLECT_MAPPER.convertValue(response, JsonNode.class);
            String fullName = node.path("chapterName").asText(null);
            if (fullName != null && !fullName.isBlank()) {
                return fullName;
            }
            return node.path("shortName").asText(null);
        } catch (Exception e) {
            log.warn("解析章节名称响应失败: {}", e.getMessage());
            return null;
        }
    }

    /** 题目内容/标准答案/要求补充（exam-api，查询失败仅记日志） */
    private void enrichQuestion(CodeDebugRequest.CodeDebugRequestBuilder builder, Long questionId) {
        if (examQuestionApi == null || questionId == null) {
            return;
        }
        try {
            Result<QuestionDetailResponse> result = examQuestionApi.getQuestionDetail(questionId);
            if (result != null && result.getCode() == ErrorCodeConstant.SUCCESS && result.getData() != null) {
                QuestionDetailResponse question = result.getData();
                builder.questionContent(question.getContent())
                        .questionStandardAnswer(question.getAnswer());
                if (question.getRequirements() != null && !question.getRequirements().isEmpty()) {
                    builder.questionRequirements(OBJECT_MAPPER.writeValueAsString(question.getRequirements()));
                }
            }
        } catch (Exception e) {
            log.warn("查询题目详情失败（AI 入参按缺失处理）: questionId={}, error={}", questionId, e.getMessage());
        }
    }

    /**
     * 运行时错误的本地行号定位：从异常堆栈中提取第一处
     * {@code at Xxx.main(Xxx.java:N)} 的行号，供 AI 调试参考。
     */
    private Integer extractErrorLineFromStack(String errorMsg) {
        if (errorMsg == null || errorMsg.isBlank()) {
            return null;
        }
        Matcher matcher = STACK_LINE_PATTERN.matcher(errorMsg);
        if (matcher.find()) {
            try {
                return Integer.valueOf(matcher.group(2));
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private String toStr(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 编译运行主流程，返回最终响应；不负责落库 */
    private CodeRunResultResponse doRun(Long userId, CodeRunRequest request) {
        long startMs = System.currentTimeMillis();
        String code = request.getCodeContent();

        // 1. 静态黑名单扫描
        Pattern matched = staticBlacklistCheck(code);
        if (matched != null) {
            log.warn("代码安全扫描拒绝: userId={}, pattern={}", userId, matched.pattern());
            return build(RunStatusEnum.SECURITY_VIOLATION, null,
                    "代码中包含禁止使用的 API（" + matched.pattern() + "），请移除后重试", null, startMs);
        }

        // 2. 结构预检：解析主类与入口方法（基于剥离注释/字符串后的代码，避免误判）
        String stripped = stripCommentsAndStrings(code);
        String mainClass = detectMainClass(stripped);
        if (mainClass == null) {
            return build(RunStatusEnum.COMPILE_ERROR, null,
                    "未找到类声明，请使用 public class Main 作为主类", null, startMs);
        }
        if (!MAIN_METHOD_PATTERN.matcher(stripped).find()) {
            return build(RunStatusEnum.COMPILE_ERROR, null,
                    "未找到程序入口，请定义 public static void main(String[] args) 方法", null, startMs);
        }

        // 3. 独立临时工作区（UUID 隔离）
        Path workDir = TEMP_ROOT.resolve("run_" + UUID.randomUUID().toString().replace("-", ""));
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            log.error("创建代码运行临时目录失败", e);
            return build(RunStatusEnum.SYSTEM_ERROR, null, "无法创建临时工作目录: " + e.getMessage(), null, startMs);
        }

        try {
            // 4. 写入源码（文件名必须与 public 类名一致）
            Path sourceFile = workDir.resolve(mainClass + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 5. 编译
            CompileOutcome compile = compile(sourceFile, workDir);
            if (!compile.success()) {
                log.info("编译失败: userId={}, errorLine={}", userId, compile.errorLine());
                return build(RunStatusEnum.COMPILE_ERROR, null, compile.diagnostics(), compile.errorLine(), startMs);
            }

            // 6. 运行（独立 JVM 进程）
            RunOutcome run = execute(workDir, mainClass);
            log.info("运行结束: userId={}, status={}", userId, run.status());

            return build(run.status(),
                    blankToNull(run.stdout()),
                    blankToNull(run.stderr()),
                    null,
                    startMs);

        } catch (IOException e) {
            log.error("代码运行 IO 异常: userId={}", userId, e);
            return build(RunStatusEnum.SYSTEM_ERROR, null, "系统内部错误: " + e.getMessage(), null, startMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return build(RunStatusEnum.SYSTEM_ERROR, null, "系统内部错误: 线程被中断", null, startMs);
        } finally {
            cleanup(workDir);
        }
    }

    // ============ 黑名单扫描 ============

    /**
     * 静态扫描：先剥离字符串字面量与注释再匹配黑名单，
     * 防止 {@code // Runtime.getRuntime()} 这类文字混淆绕过。
     */
    private Pattern staticBlacklistCheck(String code) {
        String stripped = stripCommentsAndStrings(code);
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(stripped).find()) {
                return pattern;
            }
        }
        return null;
    }

    /** 粗略剥离字符串字面量与注释，保留其余代码结构 */
    private String stripCommentsAndStrings(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            char next = i + 1 < n ? src.charAt(i + 1) : '\0';

            // 行注释
            if (c == '/' && next == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            // 块注释
            if (c == '/' && next == '*') {
                i += 2;
                while (i < n - 1 && !(src.charAt(i) == '*' && src.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, n);
                continue;
            }
            // 字符串字面量
            if (c == '"') {
                sb.append("\"\"");
                i++;
                while (i < n && src.charAt(i) != '"') {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    i++;
                }
                if (i < n) {
                    i++;
                }
                continue;
            }
            // 字符字面量
            if (c == '\'') {
                sb.append("''");
                i++;
                while (i < n && src.charAt(i) != '\'') {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        i++;
                    }
                    i++;
                }
                if (i < n) {
                    i++;
                }
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 优先取 public 类名，其次取任意类名；都没有则返回 null */
    private String detectMainClass(String strippedCode) {
        Matcher publicMatcher = PUBLIC_CLASS_PATTERN.matcher(strippedCode);
        if (publicMatcher.find()) {
            return publicMatcher.group(1);
        }
        Matcher anyMatcher = ANY_CLASS_PATTERN.matcher(strippedCode);
        if (anyMatcher.find()) {
            return anyMatcher.group(1);
        }
        return null;
    }

    // ============ 编译 ============

    /**
     * 用 JDK 内置 {@link JavaCompiler} 编译源码到 workDir。
     * 通过 Future 限时，禁用注解处理以规避处理器带来的风险。
     */
    private CompileOutcome compile(Path sourceFile, Path workDir) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new CompileOutcome(false, "当前运行环境不是 JDK，无法编译 Java 代码", null);
        }

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(collector, null, StandardCharsets.UTF_8)) {

            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(sourceFile.toFile());
            List<String> options = List.of(
                    "-encoding", "UTF-8",
                    "-proc:none",
                    "-d", workDir.toAbsolutePath().toString());
            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, collector, options, null, units);

            Future<Boolean> future = COMPILE_POOL.submit(task::call);
            boolean compiled;
            try {
                compiled = Boolean.TRUE.equals(future.get(COMPILE_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                future.cancel(true);
                return new CompileOutcome(false, "编译超时（>" + COMPILE_TIMEOUT_MS / 1000 + "s）", null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new CompileOutcome(false, "编译被中断", null);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                return new CompileOutcome(false, "编译异常: " + cause.getMessage(), null);
            }

            StringBuilder diagnostics = new StringBuilder();
            Integer firstErrorLine = null;
            for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                Diagnostic.Kind kind = diagnostic.getKind();
                if (kind != Diagnostic.Kind.ERROR
                        && kind != Diagnostic.Kind.WARNING
                        && kind != Diagnostic.Kind.MANDATORY_WARNING) {
                    continue;
                }
                if (kind == Diagnostic.Kind.ERROR && firstErrorLine == null && diagnostic.getLineNumber() > 0) {
                    firstErrorLine = (int) diagnostic.getLineNumber();
                }
                diagnostics.append(formatDiagnostic(diagnostic)).append('\n');
            }

            String message = diagnostics.toString().trim();
            if (!compiled && message.isEmpty()) {
                message = "编译失败";
            }
            return new CompileOutcome(compiled, message, firstErrorLine);

        } catch (IOException e) {
            return new CompileOutcome(false, "编译过程发生 IO 异常: " + e.getMessage(), null);
        }
    }

    /** 把单条编译诊断格式化为「[错误] Main.java:5: 找不到符号」 */
    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        StringBuilder sb = new StringBuilder();
        sb.append(diagnostic.getKind() == Diagnostic.Kind.ERROR ? "[错误]" : "[警告]").append(' ');
        if (diagnostic.getSource() != null) {
            String name = diagnostic.getSource().getName();
            try {
                Path fileName = Paths.get(name).getFileName();
                if (fileName != null) {
                    name = fileName.toString();
                }
            } catch (Exception ignore) {
                // 路径解析失败时保留原始名称
            }
            sb.append(name);
        }
        if (diagnostic.getLineNumber() > 0) {
            sb.append(':').append(diagnostic.getLineNumber());
        }
        String text = diagnostic.getMessage(Locale.CHINA);
        sb.append(": ").append(text == null ? "" : text.replace("\n", " "));
        return sb.toString();
    }

    // ============ 运行 ============

    /**
     * 启动独立 JVM 进程运行主类：限制资源、限制时间、截断输出、超时强杀。
     */
    private RunOutcome execute(Path workDir, String mainClass) throws IOException, InterruptedException {
        return execute(workDir, mainClass, null, RUN_TIMEOUT_MS);
    }

    private RunOutcome execute(Path workDir, String mainClass, String stdin, long timeoutMs)
            throws IOException, InterruptedException {
        List<String> command = List.of(
                javaExecutable(),
                MAX_HEAP_FLAG, STACK_SIZE_FLAG,
                "-Dfile.encoding=UTF-8",
                "-cp", workDir.toAbsolutePath().toString(),
                mainClass);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            return new RunOutcome(RunStatusEnum.SYSTEM_ERROR, "", "无法启动 java 进程: " + e.getMessage());
        }

        try {
            // stdin：判题时写入用例输入；自由运行时关闭，避免 Scanner 等永久阻塞
            Future<?> stdinTask = null;
            if (stdin != null && !stdin.isBlank()) {
                stdinTask = STREAM_POOL.submit(() -> writeStdin(process.getOutputStream(), stdin));
            } else {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignore) {
                    // 关闭失败不影响运行
                }
            }

            StringBuffer stdout = new StringBuffer();
            StringBuffer stderr = new StringBuffer();
            Future<?> stdoutTask = STREAM_POOL.submit(() -> readStream(process.getInputStream(), stdout, "标准输出"));
            Future<?> stderrTask = STREAM_POOL.submit(() -> readStream(process.getErrorStream(), stderr, "标准错误"));

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                cancel(stdinTask);
                stdoutTask.cancel(true);
                stderrTask.cancel(true);
                return new RunOutcome(RunStatusEnum.TIME_LIMIT_EXCEEDED, stdout.toString(),
                        "运行超时（>" + timeoutMs / 1000 + "s），已强制终止");
            }

            cancel(stdinTask);
            awaitStream(stdoutTask);
            awaitStream(stderrTask);

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return new RunOutcome(RunStatusEnum.ACCEPTED, stdout.toString(), stderr.toString());
            }
            String error = stderr.toString().isBlank()
                    ? "程序异常退出，退出码=" + exitCode
                    : stderr.toString();
            return new RunOutcome(RunStatusEnum.RUNTIME_ERROR, stdout.toString(), error);

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** 向子进程写 stdin；写完立即 flush + close，进程提前退出时写入失败属正常 */
    private void writeStdin(OutputStream out, String input) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write(input);
            if (!input.endsWith("\n")) {
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException ignore) {
            // 子进程已结束，忽略
        }
    }

    private void cancel(Future<?> task) {
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * 读取子进程输出流，超出上限只保留截断提示，避免恶意大输出撑爆内存。
     *
     * <p>注意：达到上限后必须继续读到流结束，不能直接放弃读取，
     * 否则子进程会因管道写满而阻塞，最终被误判为“运行超时”。
     */
    private void readStream(InputStream in, StringBuffer sink, String label) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[512];
            int read;
            boolean truncated = false;
            while ((read = reader.read(buffer)) != -1) {
                if (truncated) {
                    continue;
                }
                int remain = MAX_OUTPUT_CHARS - sink.length();
                if (read > remain) {
                    sink.append(buffer, 0, Math.max(remain, 0));
                    truncated = true;
                } else {
                    sink.append(buffer, 0, read);
                }
            }
            if (truncated) {
                sink.append("\n[").append(label).append("过长已截断]");
            }
        } catch (IOException e) {
            // 子进程被强杀时流会中断，属正常情况
        }
    }

    /** 等待流读取线程收尾，超时不影响主流程 */
    private void awaitStream(Future<?> task) {
        try {
            task.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignore) {
            // 流未及时读完不影响结果返回
        }
    }

    /** 定位当前 JVM 对应的 java 可执行文件，避免依赖 PATH */
    private String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            boolean windows = System.getProperty("os.name", "")
                    .toLowerCase(Locale.ROOT).contains("win");
            Path javaBin = Paths.get(javaHome, "bin", windows ? "java.exe" : "java");
            if (Files.isExecutable(javaBin)) {
                return javaBin.toString();
            }
        }
        return "java";
    }

    // ============ 清理 ============

    private void cleanup(Path workDir) {
        if (workDir == null || !Files.exists(workDir)) {
            return;
        }
        try {
            Files.walkFileTree(workDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("清理代码运行临时目录失败: {}", workDir, e);
        }
    }

    // ============ 运行记录查询（任务书 3.5 / 3.6） ============

    @Override
    public PageResult<CodeRecordItemVO> pageRecords(Long userId, CodeRecordQueryRequest request) {
        requireMapper();
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        Page<StudyCodeRunRecord> page = new Page<>(pageNum, pageSize);
        page = codeRunRecordMapper.selectPage(page,
                new LambdaQueryWrapper<StudyCodeRunRecord>()
                        .eq(StudyCodeRunRecord::getUserId, userId)
                        .eq(request.getChapterId() != null, StudyCodeRunRecord::getChapterId, request.getChapterId())
                        .eq(request.getQuestionId() != null, StudyCodeRunRecord::getQuestionId, request.getQuestionId())
                        .orderByDesc(StudyCodeRunRecord::getId));

        PageResult<CodeRecordItemVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords().stream().map(this::toItemVO).toList());
        return result;
    }

    @Override
    public CodeRecordDetailVO getRecordDetail(Long userId, Long recordId) {
        requireMapper();
        StudyCodeRunRecord record = codeRunRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, "运行记录不存在: " + recordId);
        }
        // 只能查本人记录，越权访问返回 403
        if (!userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "无权查看他人的运行记录");
        }
        return toDetailVO(record);
    }

    private void requireMapper() {
        if (codeRunRecordMapper == null) {
            throw new BusinessException("运行记录存储未装配");
        }
    }

    private CodeRecordItemVO toItemVO(StudyCodeRunRecord record) {
        CodeRecordItemVO vo = new CodeRecordItemVO();
        vo.setId(record.getId());
        vo.setChapterId(record.getChapterId());
        vo.setQuestionId(record.getQuestionId());
        if (record.getJudgeStatus() != null) {
            vo.setStatus(record.getJudgeStatus().getCode());
            vo.setStatusDesc(record.getJudgeStatus().getDesc());
        }
        vo.setJudgeResult(record.getJudgeResult() == null ? null : record.getJudgeResult().getCode());
        vo.setPassedCount(record.getPassedCount());
        vo.setTotalCount(record.getTotalCount());
        vo.setRunDuration(record.getRunDuration());
        vo.setErrorLine(record.getErrorLine());
        vo.setKnowledgePoints(record.getKnowledgePoints());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    private CodeRecordDetailVO toDetailVO(StudyCodeRunRecord record) {
        CodeRecordDetailVO vo = new CodeRecordDetailVO();
        vo.setId(record.getId());
        vo.setChapterId(record.getChapterId());
        vo.setQuestionId(record.getQuestionId());
        vo.setCodeContent(record.getCodeContent());
        vo.setRunResult(record.getRunResult());
        vo.setErrorMsg(record.getErrorMsg());
        vo.setErrorLine(record.getErrorLine());
        if (record.getJudgeStatus() != null) {
            vo.setStatus(record.getJudgeStatus().getCode());
            vo.setStatusDesc(record.getJudgeStatus().getDesc());
        }
        vo.setJudgeResult(record.getJudgeResult() == null ? null : record.getJudgeResult().getCode());
        vo.setTestResult(record.getTestResult());
        vo.setPassedCount(record.getPassedCount());
        vo.setTotalCount(record.getTotalCount());
        vo.setRunDuration(record.getRunDuration());
        vo.setMemoryUsed(record.getMemoryUsed());
        vo.setErrorLineList(record.getErrorLineList());
        vo.setErrorReason(record.getErrorReason());
        vo.setOptimizeSuggest(record.getOptimizeSuggest());
        vo.setStandardIdea(record.getStandardIdea());
        vo.setKnowledgePoints(fromKnowledgePointsJson(record.getKnowledgePoints()));
        vo.setCodeQuality(record.getCodeQuality());
        vo.setComplexity(record.getComplexity());
        vo.setSteps(record.getSteps());
        vo.setCommonMistakes(record.getCommonMistakes());
        vo.setImprovements(record.getImprovements());
        vo.setRelatedKnowledge(record.getRelatedKnowledge());
        vo.setSimilarQuestions(record.getSimilarQuestions());
        vo.setEncouragement(record.getEncouragement());
        vo.setLearningSuggestion(record.getLearningSuggestion());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /** knowledge_points 列（JSON 数组字符串）解析回列表；解析失败返回 null */
    private List<String> fromKnowledgePointsJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("知识点 JSON 解析失败: {}", json);
            return null;
        }
    }

    // ============ 工具方法 ============

    private CodeRunResultResponse build(RunStatusEnum status, String output, String errorMsg,
                                        Integer errorLine, long startMs) {
        CodeRunResultResponse response = new CodeRunResultResponse();
        response.setStatus(status.getCode());
        response.setStatusDesc(status.getDesc());
        response.setOutput(output);
        response.setErrorMsg(errorMsg);
        response.setErrorLine(errorLine);
        response.setRunDuration((int) Math.min(System.currentTimeMillis() - startMs, Integer.MAX_VALUE));
        return response;
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * 把本次运行结果写入 study_code_run_record（含全部 AI 丰富字段与判题字段），
     * 并把主键回填到响应体的 runId。
     *
     * <p>落库失败只记录日志，绝不向上抛出，避免影响学生看到编译运行结果。
     */
    private void saveRecord(Long userId, CodeRunRequest request, CodeRunResultResponse response) {
        if (codeRunRecordMapper == null) {
            return;
        }
        try {
            StudyCodeRunRecord entity = new StudyCodeRunRecord();
            entity.setUserId(userId);
            entity.setChapterId(request.getChapterId());
            entity.setQuestionId(request.getQuestionId());
            entity.setCodeContent(request.getCodeContent());
            entity.setJudgeStatus(RunStatusEnum.fromCode(response.getStatus()));
            entity.setJudgeResult(JudgeResultEnum.NOT_JUDGED);
            entity.setPassedCount(0);
            entity.setTotalCount(0);
            entity.setMemoryUsed(0);
            entity.setRunResult(response.getOutput());
            entity.setErrorMsg(response.getErrorMsg());
            entity.setErrorLine(response.getErrorLine());
            entity.setRunDuration(response.getRunDuration());
            entity.setErrorLineList(response.getErrorLineList());
            entity.setErrorReason(response.getErrorReason());
            entity.setOptimizeSuggest(response.getOptimizeSuggest());
            entity.setStandardIdea(response.getStandardIdea());
            entity.setKnowledgePoints(toJsonArray(response.getKnowledgePoints()));
            entity.setCodeQuality(response.getCodeQuality());
            entity.setComplexity(response.getComplexity());
            entity.setSteps(response.getSteps());
            entity.setCommonMistakes(response.getCommonMistakes());
            entity.setImprovements(response.getImprovements());
            entity.setRelatedKnowledge(response.getRelatedKnowledge());
            entity.setSimilarQuestions(response.getSimilarQuestions());
            entity.setEncouragement(response.getEncouragement());
            entity.setLearningSuggestion(response.getLearningSuggestion());
            codeRunRecordMapper.insert(entity);
            response.setRunId(entity.getId());
        } catch (Exception e) {
            log.error("写入代码运行记录失败: userId={}", userId, e);
        }
    }

    /** 运行/调试行为埋点（CODE_RUN / CODE_DEBUG），异步落库失败不影响主流程 */
    private void reportBehavior(Long userId, CodeRunRequest request,
                                CodeRunResultResponse response, BehaviorType type) {
        if (behaviorApi == null) {
            return;
        }
        try {
            StudyBehaviorReportRequest report = new StudyBehaviorReportRequest();
            report.setUserId(userId);
            report.setBehaviorType(type);
            report.setChapterId(request.getChapterId());
            report.setQuestionId(request.getQuestionId());
            report.setDuration(response.getRunDuration() == null ? null : response.getRunDuration() / 1000);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("status", response.getStatus());
            detail.put("runDurationMs", response.getRunDuration());
            report.setDetail(detail);
            behaviorApi.report(report);
        } catch (Exception e) {
            log.warn("{} 行为上报失败（不影响主流程）: {}", type.getCode(), e.getMessage());
        }
    }

    /** 知识点列表序列化为 JSON 数组字符串存储；空列表返回 null */
    private String toJsonArray(java.util.List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("知识点列表序列化失败: {}", list, e);
            return null;
        }
    }

    /** 对象序列化为 JSON 字符串；失败返回 null */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("JSON 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger seq = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    // ============ 内部结果载体 ============

    /** 编译结果 */
    private record CompileOutcome(boolean success, String diagnostics, Integer errorLine) {
    }

    /** 运行结果 */
    private record RunOutcome(RunStatusEnum status, String stdout, String stderr) {
    }

    /** AI 调试上下文：运行/判题结果中与调试入参相关的字段 */
    private record AiContext(RunStatusEnum status, String output, String errorMsg, Integer errorLine,
                             String testResultJson, Integer passedCount, Integer totalCount) {
    }
}
