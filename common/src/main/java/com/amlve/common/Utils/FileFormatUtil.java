package com.amlve.common.Utils;

import com.amlve.common.Constant.BusinessConstant;
import com.amlve.common.Constant.ErrorMessageConstant;
import com.amlve.common.Exception.ServerDocumentParsingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileFormatUtil {
    @Value("${python.script.docx_to_md_path}")
    private String pythonScriptPath;
    @Value("${python.script.md_to_docx_path}")
    private String mdToDocxScriptPath;
    @Value("${python.exe}")
    private String pythonExe;
    @Value("${temp.word}")
    private String tempWord;
    @Value("${temp.excel}")
    private String tempExcel;



    /**
     * 调用Python脚本转换Word文档为Markdown格式
     * @param fileUrls 文件URL列表
     * @return
     */
    public Map<String, List<String>> wordConvertToMd(List<String> fileUrls) {
        log.info("开始转换Word文档为Markdown格式");
        try {
            //结果集
            List<String> results = new ArrayList<>();

            for (String fileUrl : fileUrls) {
                List<String> command = new ArrayList<>();
                command.add(pythonExe);
                command.add(pythonScriptPath);
                command.add(fileUrl);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                Process process = pb.start();
                log.info("开始执行Python脚本: {}", command);

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(120, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException("Python脚本执行超时");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("Python脚本退出代码" + exitCode + "\nOutput:\n" + output);
                }

                results.add(output.toString().trim());
            }
            log.info("转换完成，共转换 {} 个文件", results.size());
            return Map.of(BusinessConstant.FILES, results);


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public Map<String, String>  mdConvertToWord(String content) {
        Path tempFile = null;
        // 校验输入参数
        if (null == content || content.isEmpty()) {
            throw new ServerDocumentParsingException(ErrorMessageConstant.MD_CONTENT_EMPTY);
        }
        log.info(content);

        try {
            //在系统默认临时目录下创建一个临时文件
            //前缀是 "dify_review_"，后缀是 ".json"
            tempFile = Files.createTempFile("dify_review_", ".json");

            // 将JSON 字符串以 UTF-8 编码写入这个临时文件
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            log.info("已成功创建本地临时中转文件: {}", tempFile.toAbsolutePath());

            // 构建命令行
            List<String> command = new ArrayList<>();
            command.add(pythonExe);
            command.add(mdToDocxScriptPath);
            command.add(tempFile.toAbsolutePath().toString()); // 传入路径
            command.add(tempWord); // 传入临时目录路径参数

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            log.info("开始调用 Python 脚本...");

            Process process = pb.start();
            String outputPath = "";
            // 读取 Python 控制台输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python输出] {}", line);
                    outputPath = line.trim();
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                outputPath = outputPath.split(Pattern.quote("-"))[1].trim();
                log.info("Word 文档已顺利生成。{}", outputPath);
                return Map.of(BusinessConstant.WORD_PATH, outputPath);
            } else {
                log.error("Python 脚本执行失败，退出码: {}", exitCode);
            }

        } catch (Exception e) {
            throw  new RuntimeException(e);
        } finally {
            // 无论执行成功还是失败，最后都把硬盘上的临时文件删掉
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    log.info("临时中转文件已安全自动删除。");
                } catch (Exception e) {
                    log.error("清除临时文件失败: {}", e.getMessage());
                }
            }
        }
        return null;
    }


}
