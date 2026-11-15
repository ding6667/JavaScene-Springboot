package com.amlve.agentqa.tableEnt;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生问答记录（qa_record 表）
 * <p>
 * 问答的双层存储中"永久层"：Redis 只存最近 5 轮上下文，完整对话历史与超纲统计靠本表。
 * <p>
 * 两处与库表结构强相关的刻意为之：
 * <ul>
 *     <li><b>不继承 {@code BaseEntity}</b>：qa_record 没有 update_time 列（问答只写不改），
 *         继承后 MyBatis-Plus 的自动填充会拼出不存在的列名；</li>
 *     <li><b>三列标 {@code insertStrategy = FieldStrategy.ALWAYS}</b>：
 *         is_textbook_match 的库默认值是 1、tokens_used 与 response_time 是 0。
 *         MyBatis-Plus 默认跳过 null 字段，于是"该章没配关键词所以判不了"会被库里默认成"贴合教材"、
 *         "Dify 没回 token"会被默认成"消耗 0 token"——正是任务书 6.18.1 点名的假数据。
 *         标 IGNORED 后 NULL 会被显式写入，未判定就是未判定。</li>
 * </ul>
 */
@Data
@TableName("qa_record")
public class QaRecord {

    /**
     * 主键（自增，与库表 AUTO_INCREMENT 一致）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提问学生ID（关联 sys_user.id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 章节ID，自由提问为 null
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 会话ID，一次完整对话的唯一标识
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 同一会话内的轮次序号，从 1 递增（Redis INCR 生成，DB 兜底校正）
     */
    @TableField("round_no")
    private Integer roundNo;

    /**
     * 学生问题
     */
    private String question;

    /**
     * AI 回答（已剥思考链；为空时不允许落库，见 QaAgentImpl）
     */
    private String answer;

    /**
     * 命中的本章知识点名称，逗号分隔
     */
    @TableField("knowledge_points")
    private String knowledgePoints;

    /**
     * 教材原文引用（来自知识库校验口径的命中片段，取不到为 null）
     */
    @TableField("reference_text")
    private String referenceText;

    /**
     * 是否贴合教材：1=是，0=超纲，null=该章未配关键词/原文因而无法判定
     */
    @TableField(value = "is_textbook_match", insertStrategy = FieldStrategy.ALWAYS)
    private Integer isTextbookMatch;

    /**
     * Dify 侧执行ID（工作流 run id），用于问题回溯
     */
    @TableField("dify_message_id")
    private String difyMessageId;

    /**
     * 本轮消耗 token 数，Dify 未返回时为 null（不写 0）
     */
    @TableField(value = "tokens_used", insertStrategy = FieldStrategy.ALWAYS)
    private Integer tokensUsed;

    /**
     * 端到端响应耗时（毫秒），取不到为 null（不写 0）
     */
    @TableField(value = "response_time", insertStrategy = FieldStrategy.ALWAYS)
    private Integer responseTime;

    /**
     * 提问时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
