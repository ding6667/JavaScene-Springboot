package com.amlve.teach.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * PPT 主题
 * <p>
 * 主题 CSS 作为配置数据存库，前端按 themeCode + version 动态拉取并缓存，
 * 新增/更新主题无需前端发版。
 *
 * @see TeachPptTemplate#getThemeCode()
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("teach_ppt_theme")
public class TeachPptTheme extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 主题编码，被 teach_ppt_template.theme_code 引用（全局唯一） */
    private String themeCode;

    /** 主题名称，展示用 */
    private String name;

    /** 主题外观描述，展示用 */
    private String description;

    /** 主题 CSS 全文 */
    private String cssText;

    /** 版本号，CSS 每次变更 +1，前端据此失效本地缓存 */
    private Integer version;

    /** 是否启用：1启用 0禁用 */
    @TableField("is_active")
    private Integer isActive;

    /** 排序号，越小越靠前 */
    private Integer sortOrder;


}