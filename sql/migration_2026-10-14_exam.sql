-- ============================================================
-- 迁移脚本：智能批改集成（2026-10-14）
-- 对应需求：《26.10.5.1智能批改集成》
-- 说明：
--   1. exam_question 补齐出题/刷题/批改所需字段（若字段已存在请跳过对应语句）；
--   2. study_practice_record / study_question_status 为 study 模块个人练习状态表；
--   3. exam_wrong_question 为错题归档表。
-- ============================================================

-- ---------- 1. exam_question 补字段 ----------
ALTER TABLE `exam_question` ADD COLUMN `requirements` json DEFAULT NULL COMMENT '主观题要求，JSON数组' AFTER `options`;
ALTER TABLE `exam_question` ADD COLUMN `code_framework` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编程题代码框架' AFTER `requirements`;
ALTER TABLE `exam_question` ADD COLUMN `hints` json DEFAULT NULL COMMENT '阶梯提示，JSON数组' AFTER `code_framework`;
ALTER TABLE `exam_question` ADD COLUMN `related_question_ids` json DEFAULT NULL COMMENT '相似题ID列表，JSON数组' AFTER `hints`;
ALTER TABLE `exam_question` ADD COLUMN `solution_outline` text COLLATE utf8mb4_unicode_ci COMMENT '解题思路概述' AFTER `analysis`;
ALTER TABLE `exam_question` ADD COLUMN `difficulty_score` decimal(3,1) DEFAULT NULL COMMENT '原始难度分（1.0-5.0）' AFTER `difficulty`;
ALTER TABLE `exam_question` ADD COLUMN `cognitive_level` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认知层次' AFTER `difficulty_score`;
ALTER TABLE `exam_question` ADD COLUMN `ability_dimensions` json DEFAULT NULL COMMENT '能力维度，JSON数组' AFTER `cognitive_level`;
ALTER TABLE `exam_question` ADD COLUMN `common_mistakes` json DEFAULT NULL COMMENT '常见错误，JSON数组' AFTER `ability_dimensions`;
ALTER TABLE `exam_question` ADD COLUMN `knowledge_points` json DEFAULT NULL COMMENT '知识点' AFTER `common_mistakes`;
ALTER TABLE `exam_question` ADD COLUMN `weight` decimal(5,2) DEFAULT '1.00' COMMENT '组卷权重' AFTER `score`;
ALTER TABLE `exam_question` ADD COLUMN `test_cases` json DEFAULT NULL COMMENT '测试用例，JSON数组' AFTER `deleted`;
ALTER TABLE `exam_question` ADD COLUMN `judge_mode` tinyint DEFAULT '1' COMMENT '判题方式：1=AI批改，2=测试用例判题，3=混合' AFTER `test_cases`;
ALTER TABLE `exam_question` ADD COLUMN `submit_count` int DEFAULT '0' COMMENT '总提交次数' AFTER `judge_mode`;
ALTER TABLE `exam_question` ADD COLUMN `accepted_count` int DEFAULT '0' COMMENT '通过次数' AFTER `submit_count`;
ALTER TABLE `exam_question` ADD COLUMN `acceptance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率（%）' AFTER `accepted_count`;

-- ---------- 2. study_practice_record ----------
CREATE TABLE IF NOT EXISTS `study_practice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID',
  `practice_mode` tinyint NOT NULL DEFAULT '1' COMMENT '练习模式：1=随机，2=章节专项，3=错题重做',
  `answer_content` text COLLATE utf8mb4_unicode_ci COMMENT '学生答案',
  `duration` int DEFAULT '0' COMMENT '耗时（秒）',
  `attempt_count` int DEFAULT '1' COMMENT '尝试次数',
  `is_correct` tinyint DEFAULT NULL COMMENT '是否正确：1=正确，0=错误',
  `score` int DEFAULT NULL COMMENT '得分（主观题部分得分时用）',
  `run_record_id` bigint DEFAULT NULL COMMENT '关联最后一次代码运行记录ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=草稿，1=已提交',
  `submit_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_practice_mode` (`practice_mode`),
  KEY `idx_run_record_id` (`run_record_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生自主练习记录表';

-- ---------- 3. study_question_status ----------
CREATE TABLE IF NOT EXISTS `study_question_status` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `status` tinyint DEFAULT '0' COMMENT '0=未做，1=尝试过，2=已通过',
  `attempt_count` int DEFAULT '0' COMMENT '尝试次数',
  `first_ac_time` datetime DEFAULT NULL COMMENT '首次通过时间',
  `last_submit_time` datetime DEFAULT NULL COMMENT '最后提交时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
  KEY `idx_user_status` (`user_id`,`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生题目状态表';

-- ---------- 4. exam_wrong_question ----------
CREATE TABLE IF NOT EXISTS `exam_wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `source` tinyint DEFAULT '1' COMMENT '来源：1=作业，2=刷题',
  `wrong_count` int NOT NULL DEFAULT '1' COMMENT '累计错误次数',
  `last_answer` text COLLATE utf8mb4_unicode_ci COMMENT '最近一次错误答案',
  `mastered` tinyint NOT NULL DEFAULT '0' COMMENT '是否已掌握：0=未掌握，1=已掌握',
  `last_wrong_time` datetime DEFAULT NULL COMMENT '最近答错时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`,`question_id`),
  KEY `idx_user_mastered` (`user_id`,`mastered`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题归档表';
