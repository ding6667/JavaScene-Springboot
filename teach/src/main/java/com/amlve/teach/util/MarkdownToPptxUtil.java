package com.amlve.teach.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 转 PPTX 工具（基于 marp2pptx 命令行工具）。
 * <p>
 * 转换失败（未安装、超时、退出码非0）返回 null，由调用方降级处理，不中断其余备课材料返回。
 */
@Slf4j
@Component
public class MarkdownToPptxUtil {

    /** 失败时随日志保留的子进程输出尾部行数，够定位 argparse/渲染错误即可 */
    private static final int MAX_TAIL_LINES = 20;

    /** frontmatter 分隔线（独占一行的 ---），用于判断内容是否已含完整 frontmatter */
    private static final Pattern FRONTMATTER_DELIM = Pattern.compile("(?m)^---\\s*$");

    /** CSS 注释（含 {@code @theme xxx} 声明行），解析规则前先剥离 */
    private static final Pattern CSS_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 单条 CSS 规则：selector { declarations } */
    private static final Pattern CSS_RULE = Pattern.compile("([^{}]+)\\{([^{}]*)\\}", Pattern.DOTALL);

    /** 十六进制颜色（#RGB / #RRGGBB）；rgb()/var() 等无法可靠映射到 marp2pptx，直接忽略 */
    private static final Pattern HEX_COLOR = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})\\b");

    /** 模型思考过程块（&lt;think&gt; / &lt;thinking&gt;）：属于推理泄漏，不能当正文渲染 */
    private static final Pattern REASONING_BLOCK =
            Pattern.compile("(?is)<think(?:ing)?>.*?</think(?:ing)?>");

    /** 未闭合的思考标签（输出被截断）：标签之后全部丢弃 */
    private static final Pattern REASONING_OPEN = Pattern.compile("(?is)<think(?:ing)?>.*$");

    /**
     * 转换前的 Markdown 预处理：剥离思考过程 + 保证有 Marp frontmatter。
     * <p>
     * <b>注意 marp2pptx 的实际能力</b>：它是纯 Python 的 Markdown 解析器（{@code parse_marp}
     * 自行切页），既不渲染 HTML 也不解析 CSS。因此往正文里内联 {@code <style>} 块不但不会生效，
     * 整段 CSS 还会被当成普通段落塞进第一页。主题只能在同目录同名 yaml 里生效，见
     * {@link #buildThemeYaml(String)}。
     */
    public static String prepareMarkdown(String contentMd) {
        if (contentMd == null || contentMd.isBlank()) {
            return contentMd;
        }
        return ensureFrontmatter(stripReasoning(contentMd));
    }

    /** 剥离模型思考过程（含被截断的未闭合标签），避免首页出现推理文本 */
    public static String stripReasoning(String contentMd) {
        if (contentMd == null || contentMd.isBlank()) {
            return contentMd;
        }
        String cleaned = REASONING_BLOCK.matcher(contentMd).replaceAll("");
        cleaned = REASONING_OPEN.matcher(cleaned).replaceAll("");
        // 剥离位置在开头时会留下空行，去掉才能保证 frontmatter / 首个标题仍在第一页
        return cleaned.stripLeading();
    }

    /**
     * 保证 Markdown 有 Marp frontmatter（16:9 + 页码）。已含完整 frontmatter 时原样返回，不重复注入。
     */
    public static String ensureFrontmatter(String contentMd) {
        if (contentMd == null || contentMd.isBlank() || hasFrontmatter(contentMd)) {
            return contentMd;
        }
        return "---\nmarp: true\npaginate: true\nsize: 16:9\n---\n\n" + contentMd;
    }

    /** 是否已含完整 frontmatter（首行 --- 且存在第二条 --- 闭合线） */
    private static boolean hasFrontmatter(String contentMd) {
        if (!contentMd.startsWith("---")) {
            return false;
        }
        Matcher matcher = FRONTMATTER_DELIM.matcher(contentMd);
        int found = 0;
        while (found < 2 && matcher.find()) {
            found++;
        }
        return found == 2;
    }

    /**
     * 把主题 CSS 映射成 marp2pptx 的设计配置 yaml。
     * <p>
     * marp2pptx 的样式来源只有 {@code <input>.yaml}（与 md 同目录同名）和同目录
     * {@code marp2pptx.yaml}，其余字段由内置 default.yaml 兜底合并。这里只提取能稳定映射的
     * 少量声明（强调色/正文字色/代码块底色/表头底色/中文字体），字号交给它的 auto-sizing，
     * 避免硬编码字号破坏自动排版。
     *
     * @param css 主题 CSS 全文
     * @return yaml 文本；提取不到任何可映射项时返回 null（走 marp2pptx 默认外观）
     */
    public static String buildThemeYaml(String css) {
        if (css == null || css.isBlank()) {
            return null;
        }
        Map<String, Map<String, String>> rules = parseCssRules(css);

        Map<String, String> colors = new LinkedHashMap<>();
        putColor(colors, "accent", colorOf(rules.get("h1"), "color"));
        putColor(colors, "accent_dark", colorOf(rules.get("h2"), "color"));
        putColor(colors, "text", colorOf(rules.get("section"), "color"));
        putColor(colors, "gray", colorOf(rules.get("blockquote"), "color"));
        putColor(colors, "code_bg", colorOf(rules.get("pre"), "background", "background-color"));
        putColor(colors, "table_header_bg", colorOf(rules.get("th"), "background", "background-color"));
        String mainFont = firstFontFamily(rules.get("section"));

        if (colors.isEmpty() && mainFont == null) {
            return null;
        }
        StringBuilder yaml = new StringBuilder("# 由 teach_ppt_theme.css_text 映射生成；marp2pptx 只读同名 yaml，不解析 CSS\n");
        if (!colors.isEmpty()) {
            yaml.append("colors:\n");
            colors.forEach((key, value) -> yaml.append("  ").append(key).append(": \"#").append(value).append("\"\n"));
        }
        if (mainFont != null) {
            yaml.append("fonts:\n  main: \"").append(mainFont).append("\"\n");
        }
        return yaml.toString();
    }

    /** 解析 CSS 为 selector → (property → value)；后出现的同名规则覆盖前者，与 CSS 级联方向一致 */
    private static Map<String, Map<String, String>> parseCssRules(String css) {
        Map<String, Map<String, String>> rules = new LinkedHashMap<>();
        Matcher ruleMatcher = CSS_RULE.matcher(CSS_COMMENT.matcher(css).replaceAll(""));
        while (ruleMatcher.find()) {
            String selector = ruleMatcher.group(1).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            Map<String, String> declarations = new LinkedHashMap<>();
            for (String part : ruleMatcher.group(2).split(";")) {
                int colon = part.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                declarations.put(part.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        part.substring(colon + 1).trim());
            }
            if (!declarations.isEmpty()) {
                rules.put(selector, declarations);
            }
        }
        return rules;
    }

    /** 取指定属性里的十六进制颜色，统一成 6 位大写 */
    private static String colorOf(Map<String, String> declarations, String... properties) {
        if (declarations == null) {
            return null;
        }
        for (String property : properties) {
            String value = declarations.get(property);
            if (value == null) {
                continue;
            }
            Matcher matcher = HEX_COLOR.matcher(value);
            if (!matcher.find()) {
                continue;
            }
            String hex = matcher.group(1);
            if (hex.length() == 3) {
                StringBuilder expanded = new StringBuilder(6);
                for (char c : hex.toCharArray()) {
                    expanded.append(c).append(c);
                }
                hex = expanded.toString();
            }
            return hex.toUpperCase(Locale.ROOT);
        }
        return null;
    }

    /** 取 font-family 的首个字体名（去掉引号）；形如 26px 的非字体值一律丢弃 */
    private static String firstFontFamily(Map<String, String> declarations) {
        if (declarations == null) {
            return null;
        }
        String value = declarations.get("font-family");
        if (value == null || value.isBlank()) {
            return null;
        }
        String first = value.split(",")[0].replace("\"", "").replace("'", "").trim();
        if (first.isEmpty() || first.matches("^[0-9].*")) {
            return null;
        }
        return first;
    }

    private static void putColor(Map<String, String> target, String key, String value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    /** 与 md 同目录同名的 yaml（marp2pptx 按 {@code input.with_suffix(".yaml")} 查找） */
    private static Path siblingYaml(Path mdPath) {
        String name = mdPath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return mdPath.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".yaml");
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件清理失败不影响主流程
        }
    }

    /** marp2pptx 命令（或绝对路径），未安装时转换降级 */
    @Value("${prepare.marp-command:marp2pptx}")
    private String marpCommand;

    /** 转换超时（秒）：marp2pptx 首次调用需冷启动解释器与依赖，留足余量，默认 180 */
    @Value("${prepare.marp-timeout-seconds:180}")
    private long timeoutSeconds;

    /** PPT 文件存储根目录（bootstrap 配置 temp.commonFile） */
    @Value("${temp.commonFile}")
    private String commonFileDir;

    /**
     * 将 Markdown 内容转换为 PPTX 文件（不套用主题，走 marp2pptx 默认外观）。
     * 存储路径：{temp.commonFile}/{yyyy/MM/dd}/{UUID}.pptx
     *
     * @param mdContent PPT 的 Markdown 源码（Marp 格式，页间用 --- 分隔）
     * @return 生成的 PPTX 绝对路径；转换失败返回 null
     */
    public String convertContent(String mdContent) {
        return convertContent(mdContent, null);
    }

    /**
     * 将 Markdown 内容转换为 PPTX 文件，并按主题 CSS 生成 marp2pptx 的设计配置。
     * <p>
     * 主题不能以 CSS 形式注入正文（marp2pptx 不解析 CSS），只能写成与临时 md 同目录同名的
     * yaml 供其 {@code load_config} 读取，转换结束后与 md 一并清理。
     *
     * @param mdContent PPT 的 Markdown 源码（Marp 格式，页间用 --- 分隔）
     * @param themeCss  主题 CSS 全文，空表示不套主题
     * @return 生成的 PPTX 绝对路径；转换失败返回 null
     */
    public String convertContent(String mdContent, String themeCss) {
        if (mdContent == null || mdContent.isBlank()) {
            log.warn("PPT Markdown 内容为空，跳过转换");
            return null;
        }
        Path tempMd = null;
        Path tempYaml = null;
        try {
            // 0. 预处理：剥离 <think> 推理内容 + 保证 Marp frontmatter
            String prepared = prepareMarkdown(mdContent);
            if (prepared == null || prepared.isBlank()) {
                log.warn("PPT Markdown 预处理后为空（内容仅有推理过程？），跳过转换");
                return null;
            }

            // 1. 写入系统临时 .md 文件
            tempMd = Files.createTempFile("prepare_ppt_", ".md");
            Files.writeString(tempMd, prepared, StandardCharsets.UTF_8);

            // 1.1 主题写入同名 yaml（marp2pptx 只认同名配置，CSS 对它无效）。
            //     无主题时也写一个空注释占位：load_config 会优先取同名文件并 break，
            //     这样能屏蔽临时目录里可能残留的 marp2pptx.yaml 带来的意外外观。
            tempYaml = siblingYaml(tempMd);
            String themeYaml = buildThemeYaml(themeCss);
            Files.writeString(tempYaml,
                    themeYaml != null ? themeYaml : "# 无主题：占位以屏蔽同目录 marp2pptx.yaml\n",
                    StandardCharsets.UTF_8);

            // 2. 生成目标路径 {temp.commonFile}/{yyyy/MM/dd}/{UUID}.pptx
            java.time.LocalDate today = java.time.LocalDate.now();
            String dateDir = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            String fileName = UUID.randomUUID() + ".pptx";
            Path outputDir = Paths.get(commonFileDir, dateDir);
            Files.createDirectories(outputDir);
            Path pptxPath = outputDir.resolve(fileName);

            // 3. 调用 marp2pptx 转换
            return convert(tempMd.toString(), pptxPath.toString());
        } catch (IOException e) {
            log.error("PPT 转换 IO 异常", e);
            return null;
        } finally {
            // 4. 清理临时 md / yaml，避免同一个临时目录被后续转换误用
            deleteQuietly(tempMd);
            deleteQuietly(tempYaml);
        }
    }

    /**
     * 组装 marp2pptx 转换命令。
     * <p>
     * marp2pptx 是单个位置参数的 CLI（{@code marp2pptx [-h] [-o OUTPUT] [--no-check] input}），
     * 不存在 convert 子命令：多传一个 convert 会被 argparse 当成 input，
     * 真正的 md 路径随即变成"多余参数"，进程以码 2 退出、永不产出文件。
     * 正确结构：{@code marp2pptx <md> -o <pptx>}。
     * 独立成方法便于单测断言命令结构。
     */
    java.util.List<String> buildCommand(String mdFilePath, String pptxPath) {
        return java.util.List.of(marpCommand, mdFilePath, "-o", pptxPath);
    }

    /**
     * 执行 marp2pptx 命令，将 md 文件转换为 pptx（超时秒数由 prepare.marp-timeout-seconds 配置，默认 180）
     *
     * @param mdFilePath 输入 Markdown 文件绝对路径
     * @param pptxPath   输出 PPTX 文件绝对路径
     * @return 成功返回 pptxPath；失败返回 null
     */
    private String convert(String mdFilePath, String pptxPath) {
        if (mdFilePath == null || mdFilePath.trim().isEmpty()) {
            log.error("Markdown 文件路径不能为空");
            return null;
        }
        Path mdPath = Paths.get(mdFilePath);
        if (!Files.exists(mdPath) || !Files.isRegularFile(mdPath)) {
            log.error("Markdown 文件不存在: {}", mdFilePath);
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder(buildCommand(mdFilePath, pptxPath));
        pb.redirectErrorStream(true);
        String command = String.join(" ", pb.command());

        long startTime = System.currentTimeMillis();
        try {
            Process process = pb.start();
            // marp2pptx 即使已通过文件参数拿到 Markdown，仍会阻塞等待 stdin 的 EOF；
            // 不关闭子进程输入流会一直挂到超时，PPT 转换必然失败
            process.getOutputStream().close();
            // 异步消费输出流：防止缓冲区写满阻塞子进程；
            // 同时避免子进程的孙进程（node/Marp CLI/浏览器）继承并长期持有 stdout 句柄时
            // 同步 readLine() 永久阻塞、waitFor 超时保护永远无法触发的挂死问题。
            // 保留尾部若干行，转换失败时随错误日志一并打出，否则故障现场只有退出码无从定位
            List<String> tailOutput = Collections.synchronizedList(new ArrayList<>());
            Thread outputDrainer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("marp2pptx: {}", line);
                        synchronized (tailOutput) {
                            tailOutput.add(line);
                            if (tailOutput.size() > MAX_TAIL_LINES) {
                                tailOutput.remove(0);
                            }
                        }
                    }
                } catch (IOException ignored) {
                    // 进程被销毁时流关闭属预期，忽略
                }
            }, "marp2pptx-output-drainer");
            outputDrainer.setDaemon(true);
            outputDrainer.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("marp2pptx 转换超时（{}秒），已强制终止进程, command={}", timeoutSeconds, command);
                return null;
            }
            int exitCode = process.exitValue();
            long elapsed = System.currentTimeMillis() - startTime;
            if (exitCode != 0) {
                log.error("marp2pptx 转换失败，退出码: {}, 耗时 {}ms, command={}, 输出尾部={}",
                        exitCode, elapsed, command, String.join(" | ", snapshot(tailOutput)));
                return null;
            }
            // 退出码 0 但文件缺失（--no-check 之外路径写失败等）：同样视为失败，避免下游落一条空文件记录
            if (!Files.exists(Paths.get(pptxPath))) {
                log.error("marp2pptx 返回成功但输出文件不存在: {}, command={}", pptxPath, command);
                return null;
            }
            log.info("PPTX 生成成功: {}, 耗时 {}ms", pptxPath, elapsed);
            return pptxPath;
        } catch (IOException | InterruptedException e) {
            log.error("marp2pptx 转换异常, command={}", command, e);
            return null;
        }
    }

    /** 线程安全地取出输出尾部快照（drainer 线程可能仍在写） */
    private List<String> snapshot(List<String> tailOutput) {
        synchronized (tailOutput) {
            return new ArrayList<>(tailOutput);
        }
    }
}
