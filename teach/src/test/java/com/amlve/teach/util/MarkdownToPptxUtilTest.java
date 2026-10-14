package com.amlve.teach.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownToPptxUtil 命令组装测试。
 * <p>
 * 背景：原实现调用缺少 convert 子命令（marp2pptx 为子命令式 CLI，
 * 缺失时打印帮助并以码 1 退出），导致 PPT 转换从未成功。
 */
class MarkdownToPptxUtilTest {

    private MarkdownToPptxUtil newUtil(String marpCommand) {
        MarkdownToPptxUtil util = new MarkdownToPptxUtil();
        ReflectionTestUtils.setField(util, "marpCommand", marpCommand);
        return util;
    }

    @Test
    void buildCommand_shouldContainConvertSubcommand() {
        // 命令结构：marp2pptx convert <md> -o <pptx>
        var command = newUtil("marp2pptx").buildCommand("C:/tmp/a.md", "C:/tmp/a.pptx");
        assertThat(command).containsExactly("marp2pptx", "convert", "C:/tmp/a.md", "-o", "C:/tmp/a.pptx");
    }

    @Test
    void buildCommand_shouldUseConfiguredCommandPath() {
        // marp-command 配置为绝对路径时原样使用（PATH 无 marp2pptx 的部署环境）
        var command = newUtil("D:/tools/marp2pptx.exe").buildCommand("a.md", "a.pptx");
        assertThat(command).first().isEqualTo("D:/tools/marp2pptx.exe");
        assertThat(command).element(1).isEqualTo("convert");
    }

    @Test
    void convertContent_shouldReturnNull_whenMdBlank() {
        // 空 Markdown 不触发转换
        assertThat(newUtil("marp2pptx").convertContent(null)).isNull();
        assertThat(newUtil("marp2pptx").convertContent("   ")).isNull();
    }
}
