package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教材原文新增 / 更新请求（id 为空则新增）
 * <p>
 * 字段以 knowledge_content 表结构为准：表内没有 title 与 sort 列，
 * 任务文档中的 title / sort 字段因此未采用；content_type 在库里是 tinyint 编码值
 * （1=概念定义，2=例题，3=代码案例，4=习题），不是文档写的 summary/concept 这类字符串。
 */
@Data
public class KnowledgeContentSaveRequest {

    /**
     * 主键ID，为空则新增
     */
    private Long id;

    /**
     * 所属章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 关联知识点ID（可为空）
     */
    private Long knowledgePointId;

    /**
     * 内容类型：1=概念定义，2=例题，3=代码案例，4=习题
     */
    @Min(value = 1, message = "内容类型取值范围为 1-4")
    private Integer contentType;

    /**
     * 教材原文摘录
     */
    @NotBlank(message = "教材原文不能为空")
    private String content;

    /**
     * 教材页码（int，按数值序排序）
     */
    @Min(value = 0, message = "页码不能为负数")
    private Integer page;

    /**
     * 同一页内顺序，数字越小越靠前
     */
    @Min(value = 0, message = "页内顺序不能为负数")
    private Integer pageOrder;

    /**
     * 是否同步到 Dify 知识库（默认同步）。
     * 批量导入本地素材时可先置 false，后续统一补偿同步
     */
    private Boolean syncDify = true;
}
