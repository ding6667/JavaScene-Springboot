package com.amlve.teach.constant;

import com.amlve.teach.dto.response.MarpThemeVO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    private MarpThemeConstants() {
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
        themes.add(theme("java-default", "标准教学蓝", "白底深蓝主色，适合常规课堂投影"));
        themes.add(theme("java-dark", "深色演示", "黑底白字高对比，适合暗光环境演示"));
        themes.add(theme("java-uncover", "活力渐变", "渐变背景大字排版，适合案例驱动的活泼课堂"));
        themes.add(theme("java-minimal", "极简留白", "极简排版突出内容，适合概念对比与重点提炼"));
        return List.copyOf(themes);
    }

    private static MarpThemeVO theme(String code, String name, String description) {
        MarpThemeVO vo = new MarpThemeVO();
        vo.setCode(code);
        vo.setName(name);
        vo.setDescription(description);
        return vo;
    }
}
