package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识点新增 / 更新请求（id 为空则新增）
 * <p>
 * 字段命名以 knowledge_point 表结构为准：表里是 name / difficulty / keyword / page_ref，
 * 任务文档里写的 pointName / pointLevel / difficultFlag 在库中并不存在，故未采用。
 * 知识点表保留 page_ref varchar（可存 "P.123-125" 这类区间），
 * 与 knowledge_content 的 page int 不同口径。
 */
@Data
public class KnowledgePointSaveRequest {

    /**
     * 知识点ID，为空则新增
     */
    private Long id;

    /**
     * 所属章节ID
     */
    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 知识点名称
     */
    @Size(max = 100, message = "知识点名称长度不能超过 100")
    private String name;

    /**
     * 知识点描述/定义（从教材摘录）
     */
    private String description;

    /**
     * 难度：1=了解，2=理解，3=掌握，4=重难点
     */
    @Min(value = 1, message = "难度取值范围为 1-4")
    @Max(value = 4, message = "难度取值范围为 1-4")
    private Integer difficulty;

    /**
     * 关键词（逗号分隔，如：封装,信息隐藏）
     */
    private String keyword;

    /**
     * 教材页码（如 P.123-125）
     */
    private String pageRef;

    /**
     * 同一页内顺序，数字越小越靠前
     */
    @Min(value = 0, message = "页内顺序不能为负数")
    private Integer pageOrder;
}
