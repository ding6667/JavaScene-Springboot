package com.amlve.study.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生代码运行记录（表：study_code_run_record）
 *
 * <p>每次调用「编译运行」接口都会写入一条记录，用于学情分析与 AI 调试回溯。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("study_code_run_record")
public class StudyCodeRunRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提交用户ID（来自 Token）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联章节ID（自由练习时必填）
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联题目ID（自由练习时为 null）
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 学生提交的源代码
     */
    @TableField("code_content")
    private String codeContent;

    /**
     * 运行状态：1=成功，2=编译错误，3=运行时错误，4=未通过安全检查，5=系统内部错误
     */
    private Integer status;

    /**
     * 程序运行结果（标准输出）
     */
    @TableField("run_result")
    private String runResult;

    /**
     * 错误信息（编译/运行/安全拦截/系统错误）
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 错误行号
     */
    @TableField("error_line")
    private Integer errorLine;

    /**
     * 运行耗时（毫秒）
     */
    @TableField("run_duration")
    private Integer runDuration;

    // ========== AI 调试字段（目前预留，后续由 AI 诊断服务回填） ==========

    /**
     * 错误原因（人话解释）
     */
    @TableField("error_reason")
    private String errorReason;

    /**
     * 优化建议
     */
    @TableField("optimize_suggest")
    private String optimizeSuggest;

    /**
     * 标准解题思路
     */
    @TableField("standard_idea")
    private String standardIdea;

    /**
     * 关联知识点（JSON 或逗号分隔字符串）
     */
    @TableField("knowledge_points")
    private String knowledgePoints;
}
