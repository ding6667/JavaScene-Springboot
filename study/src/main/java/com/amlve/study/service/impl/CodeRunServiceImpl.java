package com.amlve.study.service.impl;

import com.amlve.agentcodeapi.dto.request.CodeDebugRequest;
import com.amlve.agentcodeapi.dto.response.CodeDebugResponse;
import com.amlve.agentcodeapi.service.CodeAgentClient;
import com.amlve.study.dto.request.CodeRunRequest;
import com.amlve.study.dto.response.CodeRunResultResponse;
import com.amlve.study.enumeration.CodeRunStatusEnum;
import com.amlve.study.mapper.StudyCodeRunRecordMapper;
import com.amlve.study.service.CodeRunService;
import com.amlve.study.tableEnt.StudyCodeRunRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
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
 * <p>隔离与限制：
 * <ul>
 *   <li>每次请求独立临时目录，互不干扰</li>
 *   <li>子进程限制堆内存 -Xmx64m、线程栈 -Xss256k</li>
 *   <li>超时强制销毁子进程及其派生进程</li>
 *   <li>不向子进程提供标准输入，避免阻塞在输入读取上</li>
 * </ul>
 *
 * <p>注意：JDK 21 已废弃 SecurityManager（JDK 24 起移除），因此不再通过
 * {@code -Djava.security.manager} 做进程内沙箱，安全性由静态黑名单扫描 +
 * 独立子进程 + 资源/时间限制共同保证。
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

    /** AI 字段序列化（knowledgePoints 列表转 JSON 字符串落库） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ===== 运行状态 =====
    /** 运行成功 */
    private static final int STATUS_SUCCESS = 1;
    /** 编译错误 */
    private static final int STATUS_COMPILE_ERROR = 2;
    /** 运行时错误（含超时） */
    private static final int STATUS_RUNTIME_ERROR = 3;
    /** 未通过安全检查 */
    private static final int STATUS_SECURITY_REJECTED = 4;
    /** 系统内部错误 */
    private static final int STATUS_INTERNAL_ERROR = 5;

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
        // 出错（编译/运行时错误）时自动调用 AI 调试智能体，回填错误原因与优化建议
        invokeAiDebug(userId, request, response, false);
        // 每次运行都写入 study_code_run_record（含被拦截/失败的运行），落库异常不影响主流程
        saveRecord(userId, request, response);
        return response;
    }

    @Override
    public CodeRunResultResponse debugRun(Long userId, CodeRunRequest request) {
        CodeRunResultResponse response = doRun(userId, request);
        // 调试模式：无论成败都调用 AI 调试智能体（成功时 error_type=none 传运行输出）
        invokeAiDebug(userId, request, response, true);
        saveRecord(userId, request, response);
        return response;
    }

    // ============ AI 调试智能体 ============

    /**
     * 调用代码调试智能体并回填 AI 字段。
     *
     * <p>触发规则：
     * <ul>
     *   <li>{@code always=true}（调试接口）：编译错误、运行时错误、运行成功都触发</li>
     *   <li>{@code always=false}（运行接口）：仅编译错误、运行时错误触发</li>
     *   <li>安全拦截（4）/系统内部错误（5）：本地已给出明确原因，AI 无调试价值，不触发</li>
     * </ul>
     *
     * <p>智能体未装配（单测环境）或调用失败时仅记录日志，绝不影响编译运行主流程。
     */
    private void invokeAiDebug(Long userId, CodeRunRequest request,
                               CodeRunResultResponse response, boolean always) {
        if (codeAgentClient == null) {
            return;
        }
        CodeRunStatusEnum status = CodeRunStatusEnum.fromCode(response.getStatus());
        if (status == null || status == CodeRunStatusEnum.SECURITY_REJECTED
                || status == CodeRunStatusEnum.INTERNAL_ERROR) {
            return;
        }
        boolean failed = status == CodeRunStatusEnum.COMPILE_ERROR
                || status == CodeRunStatusEnum.RUNTIME_ERROR;
        if (!always && !failed) {
            return;
        }
        try {
            CodeDebugRequest debugRequest = buildDebugRequest(request, response);
            CodeDebugResponse debugResponse = codeAgentClient.debug(debugRequest);
            if (debugResponse != null) {
                response.setErrorReason(debugResponse.getErrorReason());
                response.setOptimizeSuggest(debugResponse.getOptimizeSuggest());
                response.setStandardIdea(debugResponse.getStandardIdea());
                response.setKnowledgePoints(debugResponse.getKnowledgePoints());
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
        } catch (Exception e) {
            log.warn("AI 调试智能体调用失败，不影响运行结果返回: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 把运行请求 + 运行结果组装为调试智能体请求。
     *
     * <p>章节名称、题目内容/标准答案需等题库模块就绪后补充，目前不传（Dify 端为可选变量）。
     */
    private CodeDebugRequest buildDebugRequest(CodeRunRequest request, CodeRunResultResponse response) {
        CodeRunStatusEnum status = CodeRunStatusEnum.fromCode(response.getStatus());
        String errorType = switch (status) {
            case COMPILE_ERROR -> "syntax_error";
            case RUNTIME_ERROR -> "runtime_error";
            default -> "none";
        };
        Integer errorLine = response.getErrorLine();
        if (errorLine == null) {
            // 运行时错误本地从堆栈定位行号，回传 AI 的同时回填响应（前端与落库可见）
            errorLine = extractErrorLineFromStack(response.getErrorMsg());
            if (errorLine != null) {
                response.setErrorLine(errorLine);
            }
        }
        return CodeDebugRequest.builder()
                .code(request.getCodeContent())
                .chapterId(toStr(request.getChapterId()))
                .questionId(toStr(request.getQuestionId()))
                .errorType(errorType)
                .errorMsg(response.getErrorMsg())
                .errorLine(errorLine == null ? null : String.valueOf(errorLine))
                .runResult(response.getOutput())
                .build();
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
            return build(STATUS_SECURITY_REJECTED, null,
                    "代码中包含禁止使用的 API（" + matched.pattern() + "），请移除后重试", null, startMs);
        }

        // 2. 结构预检：解析主类与入口方法（基于剥离注释/字符串后的代码，避免误判）
        String stripped = stripCommentsAndStrings(code);
        String mainClass = detectMainClass(stripped);
        if (mainClass == null) {
            return build(STATUS_COMPILE_ERROR, null,
                    "未找到类声明，请使用 public class Main 作为主类", null, startMs);
        }
        if (!MAIN_METHOD_PATTERN.matcher(stripped).find()) {
            return build(STATUS_COMPILE_ERROR, null,
                    "未找到程序入口，请定义 public static void main(String[] args) 方法", null, startMs);
        }

        // 3. 独立临时工作区（UUID 隔离）
        Path workDir = TEMP_ROOT.resolve("run_" + UUID.randomUUID().toString().replace("-", ""));
        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            log.error("创建代码运行临时目录失败", e);
            return build(STATUS_INTERNAL_ERROR, null, "无法创建临时工作目录: " + e.getMessage(), null, startMs);
        }

        try {
            // 4. 写入源码（文件名必须与 public 类名一致）
            Path sourceFile = workDir.resolve(mainClass + ".java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 5. 编译
            CompileOutcome compile = compile(sourceFile, workDir);
            if (!compile.success()) {
                log.info("编译失败: userId={}, errorLine={}", userId, compile.errorLine());
                return build(STATUS_COMPILE_ERROR, null, compile.diagnostics(), compile.errorLine(), startMs);
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
            return build(STATUS_INTERNAL_ERROR, null, "系统内部错误: " + e.getMessage(), null, startMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return build(STATUS_INTERNAL_ERROR, null, "系统内部错误: 线程被中断", null, startMs);
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
            return new RunOutcome(STATUS_INTERNAL_ERROR, "", "无法启动 java 进程: " + e.getMessage());
        }

        try {
            // 不提供标准输入：关闭子进程 stdin，避免 Scanner 等永久阻塞
            try {
                process.getOutputStream().close();
            } catch (IOException ignore) {
                // 关闭失败不影响运行
            }

            StringBuffer stdout = new StringBuffer();
            StringBuffer stderr = new StringBuffer();
            Future<?> stdoutTask = STREAM_POOL.submit(() -> readStream(process.getInputStream(), stdout, "标准输出"));
            Future<?> stderrTask = STREAM_POOL.submit(() -> readStream(process.getErrorStream(), stderr, "标准错误"));

            boolean finished = process.waitFor(RUN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                stdoutTask.cancel(true);
                stderrTask.cancel(true);
                return new RunOutcome(STATUS_RUNTIME_ERROR, stdout.toString(),
                        "运行超时（>" + RUN_TIMEOUT_MS / 1000 + "s），已强制终止");
            }

            awaitStream(stdoutTask);
            awaitStream(stderrTask);

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return new RunOutcome(STATUS_SUCCESS, stdout.toString(), stderr.toString());
            }
            String error = stderr.toString().isBlank()
                    ? "程序异常退出，退出码=" + exitCode
                    : stderr.toString();
            return new RunOutcome(STATUS_RUNTIME_ERROR, stdout.toString(), error);

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
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

    // ============ 工具方法 ============

    private CodeRunResultResponse build(int status, String output, String errorMsg,
                                       Integer errorLine, long startMs) {
        CodeRunResultResponse response = new CodeRunResultResponse();
        response.setStatus(status);
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
     * 把本次运行结果写入 study_code_run_record，并把主键回填到响应体的 runId。
     *
     * <p>AI 调试字段（errorReason/optimizeSuggest/standardIdea/knowledgePoints）
     * 由 {@link #invokeAiDebug} 在落库前回填到响应，此处一并落库。
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
            entity.setStatus(response.getStatus());
            entity.setRunResult(response.getOutput());
            entity.setErrorMsg(response.getErrorMsg());
            entity.setErrorLine(response.getErrorLine());
            entity.setRunDuration(response.getRunDuration());
            entity.setErrorReason(response.getErrorReason());
            entity.setOptimizeSuggest(response.getOptimizeSuggest());
            entity.setStandardIdea(response.getStandardIdea());
            entity.setKnowledgePoints(toJsonArray(response.getKnowledgePoints()));
            codeRunRecordMapper.insert(entity);
            response.setRunId(entity.getId());
        } catch (Exception e) {
            log.error("写入代码运行记录失败: userId={}", userId, e);
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
    private record RunOutcome(int status, String stdout, String stderr) {
    }
}
