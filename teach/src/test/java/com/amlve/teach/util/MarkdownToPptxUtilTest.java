package com.amlve.teach.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

/**
 * PPT 转换工具单元测试：本机未安装 marp2pptx 时验证降级路径。
 */
class MarkdownToPptxUtilTest {

    @TempDir
    Path tempDir;

    private MarkdownToPptxUtil buildUtil(String marpCommand) {
        MarkdownToPptxUtil util = new MarkdownToPptxUtil();
        setField(util, "marpCommand", marpCommand);
        setField(util, "commonFileDir", tempDir.resolve("commonFile").toString());
        return util;
    }

    @Test
    void convertContent_shouldReturnNull_whenContentBlank() {
        MarkdownToPptxUtil util = buildUtil("marp2pptx");

        assertThat(util.convertContent(null)).isNull();
        assertThat(util.convertContent("   ")).isNull();
    }

    @Test
    void convertContent_shouldReturnNull_whenMarpNotInstalled() {
        // 使用必然不存在的命令名，模拟 marp2pptx 未安装：应降级返回 null 而非抛异常
        MarkdownToPptxUtil util = buildUtil("definitely-not-exist-cmd-999");

        String result = util.convertContent("# PPT\n\n---\n\n# 第二页");

        assertThat(result).isNull();
    }
}
