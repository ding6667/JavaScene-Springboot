package com.amlve.study.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 代码运行记录分页查询请求（任务书 3.5）
 */
@Data
public class CodeRecordQueryRequest {

    /**
     * 章节ID（可选，为空查全部）
     */
    private Long chapterId;

    /**
     * 题目ID（可选，为空查全部）
     */
    private Long questionId;

    /**
     * 页码，从 1 开始
     */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小为1")
    private Integer pageSize = 10;
}
