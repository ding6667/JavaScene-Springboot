CREATE TABLE `class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '班级ID，主键',
  `class_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称（如：三班）',
  `class_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级编号（如：CLASS_2024_03）',
  `grade` tinyint NOT NULL COMMENT '年级（1~6 或 7~12 等）',
  `user_id` bigint NOT NULL COMMENT '负责教师ID（关联用户表）',
  `semester` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学期（如：2026-01）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-启用，0-停用',
  `course_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_code` (`class_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_class_name` (`class_name`),
  KEY `idx_semester_status` (`semester`,`status`),
  KEY `class_course_id_index` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级基础信息表'

CREATE TABLE `exam_homework_submit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID（关联teach模块）',
  `user_id` bigint NOT NULL COMMENT '提交学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `answer_content` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学生答案',
  `submit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_user_question` (`homework_id`,`user_id`,`question_id`),
  KEY `idx_homework_id` (`homework_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表'

CREATE TABLE `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `type` tinyint NOT NULL COMMENT '题型（1=选择，2=填空，3=判断，4=程序阅读，5=程序改错，6=编程题）',
  `options` json DEFAULT NULL COMMENT '选项（JSON数组，选择题用）',
  `answer` json NOT NULL COMMENT '标准答案',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度（1=基础，2=进阶，3=拔高）',
  `score` int DEFAULT '5' COMMENT '默认分值',
  `code_framework` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编程题代码骨架',
  `cognitive_level` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认知层次',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_type` (`type`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表'

CREATE TABLE `exam_question_difficulty_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '难度编码，如 basic/medium/hard/custom_001',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '难度名称，如 简单、中等、困难',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '难度说明',
  `example` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '题目示例',
  `level_order` int DEFAULT '1' COMMENT '难度排序，1最简单',
  `cognitive_levels` json DEFAULT NULL COMMENT '认知层次，如 ["记忆","理解"]',
  `knowledge_point_min` int DEFAULT '1' COMMENT '最少知识点数量',
  `knowledge_point_max` int DEFAULT '1' COMMENT '最多知识点数量',
  `allow_cross_points` tinyint DEFAULT '0' COMMENT '是否允许跨知识点，0否1是',
  `reasoning_steps_min` int DEFAULT '0' COMMENT '最少推理步骤',
  `reasoning_steps_max` int DEFAULT '1' COMMENT '最多推理步骤',
  `code_lines_min` int DEFAULT '0' COMMENT '最少代码行数',
  `code_lines_max` int DEFAULT '5' COMMENT '最多代码行数',
  `question_length_max` int DEFAULT '80' COMMENT '题干最大字数',
  `option_count` int DEFAULT '4' COMMENT '选项数量',
  `distractor_strength` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'low' COMMENT '干扰项强度：low/medium/high',
  `score_min` decimal(5,2) DEFAULT '1.00' COMMENT '难度分下限',
  `score_max` decimal(5,2) DEFAULT '2.00' COMMENT '难度分上限',
  `is_system` tinyint DEFAULT '0' COMMENT '是否系统预设',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID，NULL表示系统级',
  `chapter_id` bigint DEFAULT NULL COMMENT '绑定章节，NULL表示通用',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0禁用',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出题难度配置表'

CREATE TABLE `exam_grading_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `total_score` int NOT NULL DEFAULT '0' COMMENT '作业总分',
  `student_score` int DEFAULT NULL COMMENT '学生得分',
  `score_detail` json DEFAULT NULL COMMENT '每题得分与错误原因明细',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '批改状态：0-待批改，1-已完成',
  `error_message` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_user` (`homework_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业批改任务表'

CREATE TABLE `grading_task` (
  `id` varchar(36) NOT NULL COMMENT '批阅任务ID',
  `file_id` varchar(36) NOT NULL COMMENT '关联的文件ID（指向 upload_file）',
  `user_id` bigint DEFAULT NULL,
  `rubric_id` bigint DEFAULT NULL,
  `student_count` int DEFAULT '0' COMMENT '学生总人数',
  `processed_student_count` int DEFAULT NULL COMMENT '已完成学生作业数量',
  `status` tinyint(1) DEFAULT '0' COMMENT '0-待处理 1-处理中 2-已完成 3-已过期 4-失败(可重试)',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `error_message` text COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `task_category` varchar(20) NOT NULL DEFAULT 'HOMEWORK' COMMENT '任务类别: HOMEWORK(平时), MIDTERM(期中), FINAL(期末)',
  `task_sequence` int DEFAULT NULL COMMENT '该类任务下的序号，如第1次平时作业就填1',
  `class_id` varchar(32) NOT NULL COMMENT '所属班级ID',
  `semester` varchar(20) NOT NULL COMMENT '学期，如 2026-01',
  `file_type` varchar(16) NOT NULL COMMENT '文件类型，支持zip，word',
  `student_id` bigint DEFAULT NULL COMMENT '学生id',
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='批阅任务表'

CREATE TABLE `intent_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `draft_intent_type` varchar(32) NOT NULL COMMENT 'AI推测的意图类型',
  `draft_template_text` json NOT NULL COMMENT 'AI生成的模板问法',
  `original_user_query` varchar(500) NOT NULL COMMENT '触发该建议的用户原始问句',
  `status` tinyint DEFAULT '0' COMMENT '0-待审核, 1-已批准(已入库), 2-已拒绝',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `reviewed_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

CREATE TABLE `knowledge_chapter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint DEFAULT NULL COMMENT '所属课程ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父章节ID（0表示顶级章）',
  `chapter_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节编号（如 3.2）',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节标题',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '章节简介',
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_delete_flag` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材章节表'

CREATE TABLE `knowledge_code_snippet` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID（关联章节表）',
  `title` varchar(255) DEFAULT NULL COMMENT '代码段标题，如"数组的三种初始化方式"',
  `code` text NOT NULL COMMENT '纯代码内容，无讲解',
  `is_key_example` tinyint(1) DEFAULT '0' COMMENT '是否为重点案例',
  `page` int DEFAULT NULL COMMENT '教材页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_deleted` (`chapter_id`,`deleted`),
  KEY `idx_chapter_key_deleted` (`chapter_id`,`is_key_example`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材代码片段表'

CREATE TABLE `knowledge_concept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID（关联章节表）',
  `name` varchar(255) NOT NULL COMMENT '概念名称，如"数组的初始化"',
  `definition` text COMMENT '概念定义',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度: 1-基础, 2-进阶, 3-拔高',
  `page` int DEFAULT NULL COMMENT '教材页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_deleted` (`chapter_id`,`deleted`),
  KEY `idx_name_deleted` (`name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材核心概念表'

CREATE TABLE `knowledge_content` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `knowledge_point_id` bigint DEFAULT NULL COMMENT '关联知识点ID（可为空）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教材原文摘录',
  `content_type` tinyint DEFAULT '1' COMMENT '类型（1=概念定义，2=例题，3=代码案例，4=习题）',
  `page_ref` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `dify_document_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify知识库文档ID（关联外部）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_knowledge_point_id` (`knowledge_point_id`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_deleted` (`deleted`),
  FULLTEXT KEY `ft_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材原文内容表（AI检索用）'

CREATE TABLE `knowledge_difficult_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID（关联章节表）',
  `description` text NOT NULL COMMENT '重难点描述，如"数组下标越界的本质"',
  `type` tinyint DEFAULT '1' COMMENT '类型: 1-易错, 2-高频考点, 3-概念辨析',
  `page` int DEFAULT NULL COMMENT '教材页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_deleted` (`chapter_id`,`deleted`),
  KEY `idx_chapter_type_deleted` (`chapter_id`,`type`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材重难点表'

CREATE TABLE `knowledge_exercise` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID（关联章节表）',
  `question` text NOT NULL COMMENT '题目内容',
  `answer` text COMMENT '标准答案',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度: 1-基础, 2-进阶, 3-拔高',
  `knowledge_point` varchar(100) DEFAULT NULL COMMENT '关联知识点名称',
  `page` int DEFAULT NULL COMMENT '教材页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_deleted` (`chapter_id`,`deleted`),
  KEY `idx_chapter_difficulty_deleted` (`chapter_id`,`difficulty`,`deleted`),
  KEY `idx_knowledge_point_deleted` (`knowledge_point`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材课后习题表'

CREATE TABLE `knowledge_keyword` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '关联章节ID（NULL表示全局关键词）',
  `keyword` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关键词（如 继承、多态）',
  `synonyms` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '同义词（逗号分隔，如：封装,信息隐藏）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材关键词表（AI校验用）'

CREATE TABLE `knowledge_point` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '知识点描述/定义（从教材摘录）',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度（1=了解，2=理解，3=掌握，4=重难点）',
  `keyword` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关键词（逗号分隔，如：封装,信息隐藏）',
  `page_ref` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教材页码（如 P.123-125）',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_deleted` (`deleted`),
  FULLTEXT KEY `ft_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材知识点表'

CREATE TABLE `standard_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `standard_name` varchar(200) NOT NULL COMMENT '标准名称',
  `standard_content` json NOT NULL COMMENT '评分标准内容(Markdown)',
  `is_current` tinyint NOT NULL DEFAULT '0' COMMENT '是否当前使用: 0=否, 1=是',
  `user_id` bigint DEFAULT NULL COMMENT '创建人/来源',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_is_current` (`is_current`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评分标准配置表'

CREATE TABLE `student_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `task_id` varchar(36) NOT NULL COMMENT '关联的任务ID',
  `user_id` bigint DEFAULT NULL,
  `student_id` varchar(20) NOT NULL COMMENT '学生学号',
  `student_name` varchar(50) NOT NULL COMMENT '学生姓名',
  `raw_score` varchar(20) DEFAULT NULL COMMENT 'AI原始评分',
  `ai_comment` text COMMENT 'AI批阅评语(Markdown格式)',
  `comment_json` json NOT NULL COMMENT '评语源json',
  `teacher_score` varchar(20) DEFAULT NULL COMMENT '教师修改后的分数',
  `teacher_comment` text COMMENT '教师批阅评语',
  `teacher_note` varchar(500) DEFAULT NULL COMMENT '教师备注',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING=待批阅, PROCESSING=处理中, APPROVED=已批阅, FAILED=失败',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息(处理失败时记录)',
  `word_content_path` varchar(500) DEFAULT NULL COMMENT '学生作业路径(Markdown格式,用于重试)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_student` (`task_id`,`student_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `student_result_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `grading_task` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生批阅结果表'

CREATE TABLE `study_code_run_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `chapter_id` bigint DEFAULT NULL,
  `code_content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `run_result` text COLLATE utf8mb4_unicode_ci,
  `error_msg` text COLLATE utf8mb4_unicode_ci,
  `error_line` int DEFAULT NULL,
  `status` tinyint DEFAULT '1' COMMENT '1=成功，2=编译错误，3=运行时错误',
  `run_duration` int DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码运行记录表'

CREATE TABLE `study_learning_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `progress_rate` int DEFAULT '0' COMMENT '本章进度（0-100）',
  `study_duration` int DEFAULT '0' COMMENT '累计学习时长（秒）',
  `last_study_time` datetime DEFAULT NULL COMMENT '最后学习时间',
  `is_completed` tinyint DEFAULT '0' COMMENT '是否完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_chapter` (`user_id`,`chapter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习进度表'

CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `perm_code` varchar(100) NOT NULL COMMENT '权限编码（如：student:delete）',
  `perm_name` varchar(50) DEFAULT NULL COMMENT '权限名称（如：删除学生）',
  `parent_id` bigint DEFAULT '0' COMMENT '父级ID（用于菜单树）',
  `type` tinyint DEFAULT NULL COMMENT '类型：1-菜单，2-按钮/权限点',
  PRIMARY KEY (`id`),
  UNIQUE KEY `perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表'

CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码（如：ROLE_TEACHER）',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称（如：教师）',
  `role_desc` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表'

CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表'

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号（唯一）',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名（教师姓名）',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱（用于找回密码）',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '账号状态：0-禁用，1-启用',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除（0-未删，1-已删）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `role_id` int NOT NULL,
  `role_name` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表'

CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（外键）',
  `role_id` bigint NOT NULL COMMENT '角色ID（外键）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`) COMMENT '防止重复关联',
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表'

CREATE TABLE `teach_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班级名称',
  `teacher_id` bigint NOT NULL COMMENT '班主任/任课教师ID',
  `grade` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级（如 2024级）',
  `student_count` int DEFAULT '0' COMMENT '学生人数',
  `course_id` bigint DEFAULT NULL COMMENT '关联课程ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表'

CREATE TABLE `teach_homework` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业标题',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业内容/描述',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `teacher_id` bigint NOT NULL COMMENT '发布教师ID',
  `class_id` bigint DEFAULT NULL COMMENT '发布班级ID（NULL表示全年级）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1=未发布，2=已发布，3=已截止，4=已归档）',
  `total_score` int DEFAULT '100' COMMENT '作业总分',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_class_id` (`class_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deadline` (`deadline`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表'

CREATE TABLE `teach_homework_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID',
  `question_id` bigint NOT NULL COMMENT '题目ID（关联 exam_question）',
  `sort_order` int DEFAULT '0' COMMENT '题目排序',
  `score` int DEFAULT NULL COMMENT '本题分值（NULL则使用题目默认分值）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_question` (`homework_id`,`question_id`),
  KEY `idx_homework_id` (`homework_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业题目关联表'

CREATE TABLE `teach_ppt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名（含UUID）',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
  `md_content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PPT原始Markdown内容',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PPT文件记录表'

CREATE TABLE `teach_prepare_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `lesson_plan` longtext COLLATE utf8mb4_unicode_ci COMMENT '教案内容（Markdown）',
  `key_points` longtext COLLATE utf8mb4_unicode_ci COMMENT '重难点清单（Markdown）',
  `homework` longtext COLLATE utf8mb4_unicode_ci COMMENT '课后习题（Markdown）',
  `lab` longtext COLLATE utf8mb4_unicode_ci COMMENT '上机实训方案（Markdown）',
  `pptx_md` longtext COLLATE utf8mb4_unicode_ci COMMENT 'PPT原始Markdown内容',
  `pptx_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'PPT文件路径',
  `status` tinyint DEFAULT '1' COMMENT '状态（1=成功，0=失败）',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备课记录表'

CREATE TABLE `upload_file` (
  `id` varchar(36) NOT NULL COMMENT 'upload_ID',
  `user_id` bigint NOT NULL,
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `raw_storage_path` varchar(500) NOT NULL COMMENT '原始上传文件路径',
  `upload_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `unzip_storage_path` varchar(500) DEFAULT NULL COMMENT '解压后路径，可为空',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `file_type` varchar(16) NOT NULL COMMENT '文件类型 ',
  `expired` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='上传文件记录表'

CREATE TABLE `user_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（或团队ID）',
  `config_key` int NOT NULL COMMENT '配置项键',
  `config_value` varchar(255) NOT NULL COMMENT '配置值（JSON）',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_key` (`user_id`,`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

