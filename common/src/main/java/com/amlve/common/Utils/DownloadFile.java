package com.amlve.common.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class DownloadFile {

    private final String tempDir;

    public DownloadFile(@Value("${temp.dir}") String tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * 简化版下载
     */
    public String download(String fileUrl, String destPath) throws IOException {
        Path destination = Paths.get(destPath);
        Files.createDirectories(destination.getParent());

        HttpURLConnection httpConn = null;
        try {
            URL url = new URL(fileUrl);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(30_000);
            httpConn.setReadTimeout(300_000);
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            int responseCode = httpConn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("下载失败，HTTP响应码: " + responseCode + ", URL: " + fileUrl);
            }

            try (InputStream inputStream = httpConn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }

            log.info("文件下载完成: {}", destination.toAbsolutePath());
            return destPath;
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public String downloadTemp(String fileUrl) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".docx";
        Path destPath = Paths.get(tempDir, fileName);
        return download(fileUrl, destPath.toString());
    }
}
