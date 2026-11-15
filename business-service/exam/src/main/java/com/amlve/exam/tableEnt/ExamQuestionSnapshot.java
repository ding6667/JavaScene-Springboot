package com.amlve.exam.tableEnt;

import com.amlve.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目快照实体，对应 exam_question_snapshot 表。
 * <p>
 * 一个「题目版本」只存一行，所有学生复用，错题表里只放指针（snapshot_id），
 * 从而满足第三范式且不会因学生数增长而冗余。
 * <p>
 * 幂等键 {@code snapshotKey}：
 * <ul>
 *   <li>{@code P:{questionId}:{teachSnapshotId}} 作业发布快照，钉死在发布那一版，题库怎么改都不动；</li>
 *   <li>{@code H:{questionId}:{fingerprint}} 题库版本，内容指纹变了就是新版本。</li>
 * </ul>
 * 快照内容分两个 json 包：{@code snapshotView} 可直接下发学生，
 * {@code snapshotJudge} 含答案/解析/测试用例，只进服务端。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "exam_question_snapshot", autoResultMap = true)
public class ExamQuestionSnapshot extends BaseEntity {

    /** 快照来源：题库版本（按内容指纹去重） */
    public static final int TYPE_QUESTION_BANK = 1;

    /** 快照来源：作业发布快照（按 teach_homework_question.id 去重） */
    public static final int TYPE_HOMEWORK = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键：H:{questionId}:{fingerprint} 或 P:{questionId}:{teachSnapshotId} */
    private String snapshotKey;

    /** 快照来源：1=题库版本，2=作业发布快照 */
    private Integer snapshotType;

    /** 题库题目ID（exam_question.id） */
    private Long questionId;

    /** 作业题目快照ID（teach_homework_question.id）；题库来源为 0 */
    private Long teachSnapshotId;

    /** 内容指纹：题干+选项+标准答案的 MD5 */
    private String fingerprint;

    /** 简短题目快照 */
    private String shortTitle;

    /** 题型快照 */
    private Integer questionType;

    /** 分值快照 */
    private Integer score;

    /** 章节ID快照 */
    private Long chapterId;

    /** 难度配置ID快照 */
    private Long difficultyConfigId;

    /** 学生可见渲染包，接口可直接下发 */
    @TableField(value = "snapshot_view", typeHandler = JacksonTypeHandler.class)
    private JsonNode snapshotView;

    /** 判题与解析包，禁止下发学生端 */
    @TableField(value = "snapshot_judge", typeHandler = JacksonTypeHandler.class)
    private JsonNode snapshotJudge;

    /** 1=历史迁移数据，题面按迁移时题库回填，可能已非当时版本 */
    private Integer legacy;
}
