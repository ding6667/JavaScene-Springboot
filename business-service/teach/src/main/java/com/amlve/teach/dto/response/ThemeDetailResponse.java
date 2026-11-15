package com.amlve.teach.dto.response;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * PPT 主题详情响应
 * <p>
 * 用于前端渲染 PPT 时拉取主题 CSS。
 * 前端按 {@code themeCode + version} 做 localStorage 缓存，
 * version 变化即视为 CSS 已更新。
 */
@Data
@Accessors(chain = true)
public class ThemeDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主题编码，被 teach_ppt_template.theme_code 引用 */
    private String themeCode;

    /** 主题名称，前端展示用（如主题选择器标题） */
    private String name;

    /**
     * 主题版本号
     * <p>
     * CSS 每次变更 +1。前端把它拼进缓存 key（如 {@code ppt-theme:java-default@3}），
     * 不一致时重新拉取。
     */
    private Integer version;

    /** 主题 CSS 全文，前端注入 Shadow DOM 的 <style> 中 */
    private String cssText;

    // ---------------- 可选字段，按需保留 ----------------

    /** 主题外观描述，若前端需要显示在主题卡片上则保留 */
    private String description;

    /** 排序号，若同一份响应还用于渲染主题列表则保留 */
    private Integer sortOrder;
}