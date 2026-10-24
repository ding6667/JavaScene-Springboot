package com.amlve.teach.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主题样式表读取测试：classpath marp-themes/{themeCode}.css 的正常/降级/非法输入场景。
 */
class MarpThemeConstantsTest {

    @Test
    void stylesheet_shouldReturnCssContent_forKnownTheme() {
        // 预置主题：返回非空 CSS，含 @theme 声明与主题特征样式
        String css = MarpThemeConstants.getThemeStylesheet("java-default");
        assertThat(css).isNotNull().contains("@theme java-default").contains("section");
    }

    @Test
    void stylesheet_shouldReturnNull_forUnknownTheme() {
        // 自定义模板的未知主题码：无对应 CSS 文件，降级返回 null（调用方不注入样式）
        assertThat(MarpThemeConstants.getThemeStylesheet("no-such-theme")).isNull();
    }

    @Test
    void stylesheet_shouldReturnNull_forBlankOrNull() {
        assertThat(MarpThemeConstants.getThemeStylesheet(null)).isNull();
        assertThat(MarpThemeConstants.getThemeStylesheet("  ")).isNull();
    }

    @Test
    void stylesheet_shouldReturnNull_forIllegalCode() {
        // 非法格式（路径穿越/特殊字符）直接拒绝，不拼接资源路径
        assertThat(MarpThemeConstants.getThemeStylesheet("../application")).isNull();
        assertThat(MarpThemeConstants.getThemeStylesheet("a/b")).isNull();
        assertThat(MarpThemeConstants.getThemeStylesheet("a b")).isNull();
    }

    @Test
    void presetThemes_shouldContainAllFourThemes() {
        // 外观主题列表接口的预置元数据完整
        assertThat(MarpThemeConstants.THEMES).extracting("code")
                .containsExactlyInAnyOrder("java-default", "java-dark", "java-uncover", "java-minimal");
    }

    @Test
    void designTokens_shouldResolvePresetThemes() {
        // 模板详情 designTokens 来源：与主题 CSS 关键取值一致
        assertThat(MarpThemeConstants.getDesignTokens("java-default"))
                .containsEntry("primaryColor", "#1c1917")
                .containsEntry("backgroundColor", "#ffffff")
                .containsEntry("headingWeight", "bold");
        assertThat(MarpThemeConstants.getDesignTokens("java-dark"))
                .containsEntry("backgroundColor", "#1c1917")
                .containsEntry("textColor", "#e7e5e4");
        assertThat(MarpThemeConstants.getDesignTokens("java-uncover"))
                .containsEntry("headingWeight", "800");
        assertThat(MarpThemeConstants.getDesignTokens("java-minimal"))
                .containsEntry("headingWeight", "300");
    }

    @Test
    void designTokens_shouldReturnNull_forUnknownOrNullTheme() {
        assertThat(MarpThemeConstants.getDesignTokens("no-such-theme")).isNull();
        assertThat(MarpThemeConstants.getDesignTokens(null)).isNull();
    }
}
