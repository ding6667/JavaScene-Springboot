package com.amlve.teach.util;

import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

/**
 * PPT 就绪状态判定：列表、详情、下载三处共用同一口径。
 * <p>
 * 备课链路是「先写 pptx_md，再异步转 pptx 文件」，因此必然存在「有正文、文件还没转出来」的
 * 中间态。只看 pptx_url 是否为空无法分辨该状态（文件没转出来时该列就是空的），
 * 所以这里按「文件是否真实可读 + 是否存在正文」归并成三态。
 * <p>
 * 列表接口只暴露状态码，不暴露 pptx_md 全文（一行可能上万字，分页 10 行就是几百 KB）。
 */
public final class PptAvailabilityUtil {

    /** 无课件内容：pptx_md 为空（当初生成时 PPT 开关没开） */
    public static final int STATUS_NONE = 0;

    /** 有正文、文件未产出：异步转换中、转换失败，或文件已被清理 */
    public static final int STATUS_MD_ONLY = 1;

    /** 文件就绪：pptx_url 指向的文件真实存在，可直接读取 */
    public static final int STATUS_READY = 2;

    private PptAvailabilityUtil() {
    }

    /**
     * 是否有 PPT 正文。与 SQL 口径 {@code pptx_md IS NOT NULL AND TRIM(pptx_md) <> ''} 等价
     * （hasText 对纯空白也返回 false）。
     */
    public static boolean hasMd(String pptxMd) {
        return StringUtils.hasText(pptxMd);
    }

    /**
     * pptx_url 是否指向可读文件。
     * <p>
     * 路径非法、无访问权限、文件已被清理一律按「不可用」处理：这种情况下返回给前端的路径
     * 就是会 404 的死链，宁可判定为未产出，由下载接口用 pptx_md 现场补生成。
     */
    public static boolean isFileReady(String pptxUrl) {
        if (!StringUtils.hasText(pptxUrl)) {
            return false;
        }
        try {
            return Files.exists(Paths.get(pptxUrl));
        } catch (InvalidPathException | SecurityException e) {
            return false;
        }
    }

    /**
     * 归并为三态：文件就绪优先，其次看是否有正文。
     *
     * @param pptxMd  PPT 原始 Markdown（库里的 pptx_md）
     * @param pptxUrl PPT 文件路径（库里的 pptx_url，可能是死链）
     */
    public static int resolve(String pptxMd, String pptxUrl) {
        if (isFileReady(pptxUrl)) {
            return STATUS_READY;
        }
        return hasMd(pptxMd) ? STATUS_MD_ONLY : STATUS_NONE;
    }
}
