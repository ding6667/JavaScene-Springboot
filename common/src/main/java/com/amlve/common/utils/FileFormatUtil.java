package com.amlve.common.utils;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.constant.BusinessConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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


    public Map<String, List<String>> word2Md(List<String> fileUrls) {
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

    public byte[] generateWord(String content) {
        Path jsonTempFile = null;
        Path wordTempFile = null;
        String uid = UUID.randomUUID().toString();

        if (content == null || content.isEmpty()) {
            throw new BusinessException("Markdown内容不能为空");
        }

        try {
            jsonTempFile = Files.createTempFile("dify_review_" + uid , ".json");
            Files.writeString(jsonTempFile, content, StandardCharsets.UTF_8);
            log.info("JSON 临时文件创建: {}", jsonTempFile.toAbsolutePath());

            wordTempFile = Files.createTempFile("word_output_" + uid, ".docx");
            log.info("Word 临时文件路径: {}", wordTempFile.toAbsolutePath());

            List<String> command = new ArrayList<>();
            command.add(pythonExe);
            command.add(mdToDocxScriptPath);
            command.add(jsonTempFile.toAbsolutePath().toString());
            command.add(wordTempFile.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            log.info("调用 Python 脚本: {}", String.join(" ", command));

            Process process = pb.start();


            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python 脚本执行失败，退出码: " + exitCode);
            }

            byte[] wordBytes = Files.readAllBytes(wordTempFile);
            log.info("Word 生成成功，大小: {} 字节", wordBytes.length);

            return wordBytes;

        } catch (Exception e) {
            log.error("Word 转换失败", e);
            throw new RuntimeException("Word 转换失败: " + e.getMessage(), e);
        } finally {
            cleanTempFile(jsonTempFile);
            cleanTempFile(wordTempFile);
        }
    }

    private void cleanTempFile(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                Files.delete(path);
                log.info("临时文件已删除: {}", path.toAbsolutePath());
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", path.toAbsolutePath(), e);
            }
        }
    }
    public static byte[] generateScoreExcel(List<StudentScore> results, String sheetName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"序号", "学号", "姓名", "分数"};
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (int i = 0; i < results.size(); i++) {
                StudentScore item = results.get(i);
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(item.getStudentId());
                row.createCell(2).setCellValue(item.getStudentName());

                String score = item.getScore();
                row.createCell(3).setCellValue(score != null ? score : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("生成Excel文件失败", e);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StudentScore {
        private String studentId;
        private String studentName;
        private String score;
    }

}
