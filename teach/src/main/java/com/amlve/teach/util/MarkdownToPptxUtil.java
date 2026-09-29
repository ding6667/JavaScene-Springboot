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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Markdown 转 PPTX 工具（基于 marp2pptx 命令行工具）。
 * <p>
 * 转换失败（未安装、超时、退出码非0）返回 null，由调用方降级处理，不中断其余备课材料返回。
 */
@Slf4j
@Component
public class MarkdownToPptxUtil {

    /** marp2pptx 命令（或绝对路径），未安装时转换降级 */
    @Value("${prepare.marp-command:marp2pptx}")
    private String marpCommand;

    /** 转换超时（秒）：marp2pptx 内部经 npx 调 Marp CLI 并驱动浏览器渲染，耗时较长，默认 180 */
    @Value("${prepare.marp-timeout-seconds:180}")
    private long timeoutSeconds;

    /** PPT 文件存储根目录（bootstrap 配置 temp.commonFile） */
    @Value("${temp.commonFile}")
    private String commonFileDir;

    /**
     * 将 Markdown 内容转换为 PPTX 文件。
     * 存储路径：{temp.commonFile}/{yyyy/MM/dd}/{UUID}.pptx
     *
     * @param mdContent PPT 的 Markdown 源码（Marp 格式，页间用 --- 分隔）
     * @return 生成的 PPTX 绝对路径；转换失败返回 null
     */
    public String convertContent(String mdContent) {
        if (mdContent == null || mdContent.isBlank()) {
            log.warn("PPT Markdown 内容为空，跳过转换");
            return null;
        }
        Path tempMd = null;
        try {
            // 1. 写入系统临时 .md 文件
            tempMd = Files.createTempFile("prepare_ppt_", ".md");
            Files.writeString(tempMd, mdContent, StandardCharsets.UTF_8);

            // 2. 生成目标路径 {temp.commonFile}/{yyyy/MM/dd}/{UUID}.pptx
            java.time.LocalDate today = java.time.LocalDate.now();
            String dateDir = String.format("%d/%02d/%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
            String fileName = UUID.randomUUID() + ".pptx";
            Path outputDir = Paths.get(commonFileDir, dateDir);
            Files.createDirectories(outputDir);
            Path pptxPath = outputDir.resolve(fileName);

            // 3. 调用 marp2pptx 转换
            String result = convert(tempMd.toString(), pptxPath.toString());
            return result;
        } catch (IOException e) {
            log.error("PPT 转换 IO 异常", e);
            return null;
        } finally {
            // 4. 清理临时 md 文件
            if (tempMd != null) {
                try {
                    Files.deleteIfExists(tempMd);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响主流程
                }
            }
        }
    }

    /**
     * 执行 marp2pptx 命令，将 md 文件转换为 pptx（30 秒超时）
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

        // marp2pptx 为子命令式 CLI：marp2pptx convert <md> -o <pptx>
        ProcessBuilder pb = new ProcessBuilder(marpCommand,  mdFilePath, "-o", pptxPath);
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        try {
            Process process = pb.start();
            // 异步消费输出流：防止缓冲区写满阻塞子进程；
            // 同时避免子进程的孙进程（node/Marp CLI/浏览器）继承并长期持有 stdout 句柄时
            // 同步 readLine() 永久阻塞、waitFor 超时保护永远无法触发的挂死问题
            Thread outputDrainer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("marp2pptx: {}", line);
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
                log.error("marp2pptx 转换超时（{}秒），已强制终止进程", timeoutSeconds);
                return null;
            }
            int exitCode = process.exitValue();
            long elapsed = System.currentTimeMillis() - startTime;
            if (exitCode == 0) {
                log.info("PPTX 生成成功: {}, 耗时 {}ms", pptxPath, elapsed);
                return pptxPath;
            } else {
                log.error("marp2pptx 转换失败，退出码: {}, 耗时 {}ms", exitCode, elapsed);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("marp2pptx 转换异常", e);
            return null;
        }
    }
}
