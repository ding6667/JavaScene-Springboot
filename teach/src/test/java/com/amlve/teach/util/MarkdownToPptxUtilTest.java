package com.amlve.teach.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownToPptxUtil 命令组装测试。
 * <p>
 * 背景：marp2pptx 是单位置参数的 CLI（marp2pptx [-h] [-o OUTPUT] [--no-check] input），
 * 不存在 convert 子命令。曾误按子命令式写成 {@code marp2pptx convert <md> -o <pptx>}，
 * argparse 会把 convert 当成 input、把真实 md 路径判为"多余参数"，以码 2 退出，
 * 导致 PPT 文件永不生成、pptxUrl 不回填、teach_ppt 不落记录。
 */
class MarkdownToPptxUtilTest {

    private MarkdownToPptxUtil newUtil(String marpCommand) {
        MarkdownToPptxUtil util = new MarkdownToPptxUtil();
        ReflectionTestUtils.setField(util, "marpCommand", marpCommand);
        return util;
    }

    @Test
    void buildCommand_shouldNotContainConvertSubcommand() {
        // 命令结构：marp2pptx <md> -o <pptx>（多一个 convert 会导致 argparse 退出码 2）
        var command = newUtil("marp2pptx").buildCommand("C:/tmp/a.md", "C:/tmp/a.pptx");
        assertThat(command).containsExactly("marp2pptx", "C:/tmp/a.md", "-o", "C:/tmp/a.pptx");
        assertThat(command).doesNotContain("convert");
    }

    @Test
    void buildCommand_shouldUseConfiguredCommandPath() {
        // marp-command 配置为绝对路径时原样使用（PATH 无 marp2pptx 的部署环境）
        var command = newUtil("D:/tools/marp2pptx.exe").buildCommand("a.md", "a.pptx");
        assertThat(command).first().isEqualTo("D:/tools/marp2pptx.exe");
        assertThat(command).element(1).isEqualTo("a.md");
        assertThat(command).last().isEqualTo("a.pptx");
    }

    @Test
    void convertContent_shouldReturnNull_whenMdBlank() {
        // 空 Markdown 不触发转换
        assertThat(newUtil("marp2pptx").convertContent(null)).isNull();
        assertThat(newUtil("marp2pptx").convertContent("   ")).isNull();
    }
}
