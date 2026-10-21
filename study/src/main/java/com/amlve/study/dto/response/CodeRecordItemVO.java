package com.amlve.study.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码运行记录列表项（任务书 3.5，不返回完整代码，避免列表过重）
 */
@Data
public class CodeRecordItemVO {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 章节ID
     */
    private Long chapterId;

    /**
     * 题目ID（null=自由练习）
     */
    private Long questionId;

    /**
     * 运行状态：AC/WA/TLE/MLE/RE/CE/OLE/SV/SE
     */
    private String status;

    /**
     * 运行状态中文描述
     */
    private String statusDesc;

    /**
     * 判题结果：1=全部通过，0=未通过，null=未判题
     */
    private Integer judgeResult;

    /**
     * 通过用例数
     */
    private Integer passedCount;

    /**
     * 总用例数
     */
    private Integer totalCount;

    /**
     * 运行耗时（毫秒）
     */
    private Integer runDuration;

    /**
     * 错误行号（编译器报的单个）
     */
    private Integer errorLine;

    /**
     * 关联知识点（逗号分隔）
     */
    private String knowledgePoints;

    /**
     * 运行时间
     */
    private LocalDateTime createTime;
}
