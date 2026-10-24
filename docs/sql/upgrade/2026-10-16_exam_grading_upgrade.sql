-- =============================================================================
-- 26.10.5.1 智能批改集成 —— 已有库增量升级脚本
-- 负责人：潘魏诚
-- 涉及模块：exam
-- 说明：
--   1) 干净库请直接执行 docs/sql/table.sql（已包含以下全部结构），无需执行本脚本；
--   2) 已有库执行本脚本，补齐 exam_question 出题字段与批改/错题两张新表；
--   3) 若目标列/表已存在，MySQL 会明确报 Duplicate column / Table exists，
--      不会静默跳过；此时请自行移除对应语句后重跑。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. exam_question 补齐出题智能体新增字段（11 列，见需求 5.2 / 5.3）
-- -----------------------------------------------------------------------------
ALTER TABLE `exam_question`
    ADD COLUMN `hints` JSON DEFAULT NULL COMMENT '阶梯提示，JSON数组' AFTER `code_framework`,
    ADD COLUMN `related_question_ids` JSON DEFAULT NULL COMMENT '相似题ID列表' AFTER `hints`,
    ADD COLUMN `difficulty_score` DECIMAL(3, 1) DEFAULT NULL COMMENT '原始难度分（1.0-5.0）' AFTER `difficulty`,
    ADD COLUMN `cognitive_level` VARCHAR(20) DEFAULT NULL COMMENT '认知层次' AFTER `difficulty_score`,
    ADD COLUMN `ability_dimensions` JSON DEFAULT NULL COMMENT '能力维度' AFTER `cognitive_level`,
    ADD COLUMN `common_mistakes` JSON DEFAULT NULL COMMENT '常见错误' AFTER `ability_dimensions`,
    ADD COLUMN `solution_outline` TEXT DEFAULT NULL COMMENT '解题思路概述' AFTER `analysis`,
    ADD COLUMN `weight` DECIMAL(5, 2) DEFAULT 1.00 NULL COMMENT '组卷权重' AFTER `score`,
    ADD COLUMN `submit_count` INT DEFAULT 0 NULL COMMENT '总提交次数' AFTER `judge_mode`,
    ADD COLUMN `accepted_count` INT DEFAULT 0 NULL COMMENT '通过次数' AFTER `submit_count`,
    ADD COLUMN `acceptance_rate` DECIMAL(5, 2) DEFAULT 0.00 NULL COMMENT '通过率（%）' AFTER `accepted_count`;

-- -----------------------------------------------------------------------------
-- 2. 作业批改任务表（按学生+作业汇总，见需求 5.3）
-- -----------------------------------------------------------------------------
create table exam_grading_task
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    homework_id   bigint                             not null comment '作业ID（关联 teach_homework）',
    user_id       bigint                             not null comment '提交学生ID',
    total_score   int      default 100               null comment '作业满分',
    student_score int                                null comment '学生总分',
    score_detail  json                               null comment '单题得分与错误原因，JSON数组',
    status        tinyint  default 0                 not null comment '批改状态：0=待批改，1=已完成，2=批改失败',
    error_message varchar(2000)                      null comment '批改失败原因',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_homework_user
        unique (homework_id, user_id)
)
    comment '作业批改任务表（按学生+作业汇总）' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on exam_grading_task (deleted);

create index idx_homework_id
    on exam_grading_task (homework_id);

create index idx_user_id
    on exam_grading_task (user_id);

-- -----------------------------------------------------------------------------
-- 3. 错题归档表（见需求 5.3）
-- -----------------------------------------------------------------------------
create table exam_wrong_question
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    user_id         bigint                             not null comment '学生ID',
    question_id     bigint                             not null comment '题目ID',
    source          tinyint  default 1                 null comment '来源：1=作业，2=刷题',
    wrong_count     int      default 0                 not null comment '累计错误次数',
    last_answer     text                               null comment '最近一次错误答案',
    mastered        tinyint  default 0                 not null comment '是否已掌握：0=未掌握，1=已掌握',
    last_wrong_time datetime                           null comment '最近答错时间',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_user_question
        unique (user_id, question_id)
)
    comment '错题归档表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on exam_wrong_question (deleted);

create index idx_question_id
    on exam_wrong_question (question_id);

create index idx_user_id
    on exam_wrong_question (user_id);