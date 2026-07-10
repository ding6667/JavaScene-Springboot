package com.amlve.common.Utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class ZipUtil {

    /**
     * 递归查找目录下的 Word 文档（.docx 或 .doc）
     */
    public static File findWordFile(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findWordFile(f);
                if (found != null) return found;
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".docx") || name.endsWith(".doc")) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * 解压 一层 Zip 文件到目标目录
     */
    public static void unzip(String zipFilePath, File destDir) throws IOException {
        destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                validatePath(destDir, outFile, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 递归解压 Zip 文件，将所有嵌套的 zip 也一并解压到目标目录
     * @param zipFilePath 源 zip 文件路径
     * @param destDir    目标目录
     * @param maxDepth   最大递归深度，防止 zip bomb
     */
    public static void unzipRecursive(String zipFilePath, File destDir, int maxDepth) throws IOException {
        if (maxDepth <= 0) {
            throw new IOException("递归解压超过最大深度限制，可能存在 zip bomb");
        }
        destDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                validatePath(destDir, outFile, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    if (outFile.getName().toLowerCase().endsWith(".zip")) {
                        File subDestDir = new File(outFile.getParentFile(),
                                outFile.getName().replaceAll("(?i)\\.zip$", ""));
                        unzipRecursive(outFile.getAbsolutePath(), subDestDir, maxDepth - 1);
                        outFile.delete();
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void validatePath(File destDir, File outFile, String entryName) throws IOException {
        Path destPath = destDir.toPath().toAbsolutePath().normalize();
        Path resolvedPath = outFile.toPath().toAbsolutePath().normalize();
        if (!resolvedPath.startsWith(destPath)) {
            throw new IOException("非法条目: " + entryName + "，可能存在路径遍历攻击(Zip Slip)");
        }
    }

}