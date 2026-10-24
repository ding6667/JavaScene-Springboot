package com.amlve.teach.constant;

import com.amlve.teach.dto.response.MarpThemeVO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Marp 外观主题常量：预置主题元数据 + 主题样式表（CSS）读取。
 * <p>
 * 主题 CSS 位于 classpath marp-themes/{themeCode}.css，文件头以 {@code /* @theme <code> * /}
 * 声明主题名（与 themeCode 一致）。marp2pptx 不支持向 Marp CLI 透传 --theme-set，
 * 自定义主题经 {@code <style>} 块内联进 Markdown 生效（见 TeachPrepareServiceImpl#injectTheme），
 * 因此无需维护 themeCode → Marp 内置主题名的映射。
 */
public final class MarpThemeConstants {

    /** themeCode 合法格式：字母/数字/连字符（防目录穿越与非法资源路径） */
    private static final Pattern THEME_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9-]+$");

    /** 主题 CSS 的 classpath 根目录 */
    private static final String THEME_CSS_DIR = "marp-themes/";

    /** 预置主题元数据（外观主题列表接口直接返回，不查库） */
    public static final List<MarpThemeVO> THEMES = buildThemes();

    /**
     * 预置主题的设计令牌：模板未配置 design_tokens 时按 themeCode 回退，
     * 保证模板详情与备课生成始终有令牌可用。
     */
    private static final Map<String, Map<String, Object>> DESIGN_TOKENS = buildDesignTokens();

    private MarpThemeConstants() {
    }

    /**
     * 读取主题设计令牌（模板详情 designTokens 字段来源）。
     *
     * @param themeCode 主题编码
     * @return 只读令牌 Map；未知主题返回 null
     */
    public static Map<String, Object> getDesignTokens(String themeCode) {
        if (themeCode == null) {
            return null;
        }
        return DESIGN_TOKENS.get(themeCode);
    }

    /**
     * 读取主题样式表内容。
     *
     * @param themeCode 模板主题码（如 java-default）
     * @return CSS 文本；themeCode 为空、格式非法、无对应文件或读取失败时返回 null（调用方降级为不注入样式）
     */
    public static String getThemeStylesheet(String themeCode) {
        if (themeCode == null || themeCode.isBlank()
                || !THEME_CODE_PATTERN.matcher(themeCode).matches()) {
            return null;
        }
        String path = THEME_CSS_DIR + themeCode + ".css";
        try (InputStream in = MarpThemeConstants.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static List<MarpThemeVO> buildThemes() {
        List<MarpThemeVO> themes = new ArrayList<>();
        themes.add(theme("java-default", "标准教学", "白底黑字，克制清晰，适合常规课堂投影"));
        themes.add(theme("java-dark", "深色演示", "深灰底浅字，适合弱光环境与投影对比"));
        themes.add(theme("java-uncover", "案例大字", "浅灰底超大字标题，适合案例拆解与重点强调"));
        themes.add(theme("java-minimal", "极简留白", "暖白底细字重，留白充足，适合概念讲解"));
        return List.copyOf(themes);
    }

    private static MarpThemeVO theme(String code, String name, String description) {
        MarpThemeVO vo = new MarpThemeVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setDescription(description);
        return vo;
    }

    /**
     * 预置主题设计令牌：取值与 marp-themes/{themeCode}.css 中的关键样式保持一致，
     * 供前端模板详情展示与备课生成时的视觉覆写使用。
     */
    private static Map<String, Map<String, Object>> buildDesignTokens() {
        Map<String, Map<String, Object>> tokens = new LinkedHashMap<>();
        tokens.put("java-default", tokens("#1c1917", "#ffffff", "#333333",
                "'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', sans-serif", "bold", "#f5f5f4",
                "白底黑字，克制清晰，适合常规课堂投影"));
        tokens.put("java-dark", tokens("#ffffff", "#1c1917", "#e7e5e4",
                "'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', sans-serif", "bold", "#292524",
                "深灰底浅字，适合弱光环境与投影对比"));
        tokens.put("java-uncover", tokens("#000000", "#f5f5f4", "#1c1917",
                "'Microsoft YaHei', 'PingFang SC', 'Helvetica Neue', sans-serif", "800", "#e7e5e4",
                "浅灰底超大字标题，适合案例拆解与重点强调"));
        tokens.put("java-minimal", tokens("#292524", "#fafaf9", "#44403c",
                "'Noto Serif SC', Georgia, 'Times New Roman', serif", "300", "#f0eeec",
                "暖白底细字重，留白充足，适合概念讲解"));
        return Map.copyOf(tokens);
    }

    private static Map<String, Object> tokens(String primaryColor, String backgroundColor, String textColor,
                                              String fontFamily, String headingWeight, String codeBgColor,
                                              String styleDescription) {
        Map<String, Object> token = new LinkedHashMap<>();
        token.put("primaryColor", primaryColor);
        token.put("backgroundColor", backgroundColor);
        token.put("textColor", textColor);
        token.put("fontFamily", fontFamily);
        token.put("headingWeight", headingWeight);
        token.put("codeBgColor", codeBgColor);
        token.put("styleDescription", styleDescription);
        return java.util.Collections.unmodifiableMap(token);
    }
}
