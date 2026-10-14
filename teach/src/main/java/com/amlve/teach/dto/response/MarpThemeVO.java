package com.amlve.teach.dto.response;

import lombok.Data;

/**
 * PPT 外观主题项（对应 marp-themes 目录下一套 CSS 主题）
 */
@Data
public class MarpThemeVO {

    /** 主题编码（同时是 CSS 文件名与 teach_ppt_template.theme_code 的合法取值） */
    private String code;

    /** 主题名称 */
    private String name;

    /** 风格说明 */
    private String description;
}
