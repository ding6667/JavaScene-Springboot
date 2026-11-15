-- ============================================================
-- JavaScene 微服务分库一键初始化脚本
-- 生成日期: 2026-11-08
-- 内容: 7 个服务库 + 39 张表结构 + 系统必须种子数据
-- 不含: 运行时/测试产生的业务数据（用户/班级/教材/练习/问答记录等）
-- 幂等: CREATE DATABASE/TABLE IF NOT EXISTS + INSERT IGNORE，可重复执行
-- 用法: mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 < init_sharded_databases.sql
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 数据库: javascene_system
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_system` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_system`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_admin_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联 sys_user.id',
  `admin_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '管理员工号',
  `permission_level` tinyint DEFAULT '1' COMMENT '权限级别：1=普通管理员，2=超级管理员',
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员信息扩展表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_class` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭ID',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鐝?骇鍚嶇О',
  `teacher_id` bigint DEFAULT NULL COMMENT '浠昏?鏁欏笀锛屽叧鑱?sys_user.id',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '骞寸骇锛屽? 2024绾',
  `student_count` int DEFAULT '0' COMMENT '瀛︾敓浜烘暟锛屾湇鍔＄?缁熻?',
  `course_id` bigint DEFAULT NULL COMMENT '鎵?睘璇剧▼ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐝?骇涓绘暟鎹?〃';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `perm_code` varchar(100) NOT NULL COMMENT '权限编码（如：student:delete）',
  `perm_name` varchar(50) DEFAULT NULL COMMENT '权限名称（如：删除学生）',
  `parent_id` bigint DEFAULT '0' COMMENT '父级ID（用于菜单树）',
  `type` tinyint DEFAULT NULL COMMENT '类型：1-菜单，2-按钮/权限点',
  PRIMARY KEY (`id`),
  UNIQUE KEY `perm_code` (`perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码（如：ROLE_TEACHER）',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称（如：教师）',
  `role_desc` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_student_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联 sys_user.id',
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号（唯一）',
  `class_id` bigint DEFAULT NULL COMMENT '班级ID',
  `class_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名称（冗余）',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级（如 2024级）',
  `major` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '专业',
  `college` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学院',
  `enroll_year` int DEFAULT NULL COMMENT '入学年份',
  `graduation_year` int DEFAULT NULL COMMENT '毕业年份（如 2028）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_student_no` (`student_no`),
  KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息扩展表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_teacher_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联 sys_user.id',
  `teacher_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工号（唯一）',
  `title` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称（讲师/副教授/教授）',
  `college` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属学院',
  `department` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所属系/教研室',
  `office` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '办公室',
  `research_area` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '研究方向',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师信息扩展表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号（唯一）',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名（教师姓名）',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱（用于找回密码）',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '账号状态：0-禁用，1-启用',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除（0-未删，1-已删）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `role_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID（外键）',
  `role_id` bigint NOT NULL COMMENT '角色ID（外键）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`) COMMENT '防止重复关联',
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `standard_config` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评分标准配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- 种子数据: sys_role
USE `javascene_system`;
INSERT IGNORE INTO `sys_role` (`id`, `role_code`, `role_name`, `role_desc`, `create_time`, `update_time`) VALUES (1,'1001','STUDENT','普通学生','2026-09-04 16:10:13','2026-09-04 16:10:21'),(2,'1002','TEACHER','普通教师','2026-09-04 16:10:46','2026-09-04 16:10:46'),(3,'1003','ADMIN','系统管理员','2026-09-04 16:11:18','2026-10-25 15:23:55');

-- ------------------------------------------------------------
-- 数据库: javascene_study
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_study` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_study`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `study_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `behavior_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行为类型（BehaviorType.code，如 code_debug）',
  `chapter_id` bigint DEFAULT NULL COMMENT '关联章节ID',
  `question_id` bigint DEFAULT NULL COMMENT '关联题目ID',
  `duration` int DEFAULT '0' COMMENT '行为耗时（秒）',
  `detail` json DEFAULT NULL COMMENT '行为详情（JSON）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0=未删，1=已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_type_time` (`user_id`,`behavior_type`,`create_time`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习行为记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `study_code_run_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '关联章节ID',
  `question_id` bigint DEFAULT NULL COMMENT '关联题目ID（NULL=自由练习）',
  `code_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户提交的代码',
  `run_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '运行输出结果',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '编译/运行时错误信息',
  `error_line` int DEFAULT NULL COMMENT '错误行号',
  `error_line_list` json DEFAULT NULL COMMENT '错误行号列表（AI 返回）',
  `status` tinyint DEFAULT '1' COMMENT '1=成功，2=编译错误，3=运行时错误',
  `judge_status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'AC/WA/TLE/MLE/RE/CE/OLE/SV/SE',
  `judge_result` tinyint DEFAULT '-1' COMMENT '1=全通过，0=部分通过，-1=未判题',
  `test_result` json DEFAULT NULL COMMENT '测试用例执行结果',
  `passed_count` int DEFAULT '0' COMMENT '通过用例数',
  `total_count` int DEFAULT '0' COMMENT '总用例数',
  `run_duration` int DEFAULT '0' COMMENT '运行耗时（毫秒）',
  `memory_used` int DEFAULT '0' COMMENT '内存占用（KB）',
  `error_reason` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误原因（AI 调试返回）',
  `optimize_suggest` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '优化建议（AI 调试返回）',
  `standard_idea` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标准解题思路（AI 调试返回）',
  `knowledge_points` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联知识点（逗号分隔）',
  `code_quality` json DEFAULT NULL COMMENT '代码质量评分',
  `complexity` json DEFAULT NULL COMMENT '复杂度分析',
  `steps` json DEFAULT NULL COMMENT '分步引导',
  `common_mistakes` json DEFAULT NULL COMMENT '常见错误对比',
  `improvements` json DEFAULT NULL COMMENT '改进建议',
  `related_knowledge` json DEFAULT NULL COMMENT '关联教材',
  `similar_questions` json DEFAULT NULL COMMENT '相似题推荐',
  `encouragement` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '鼓励性话语',
  `learning_suggestion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '学习建议',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代码运行记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `study_learning_progress` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习进度表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `study_practice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID',
  `practice_mode` tinyint NOT NULL DEFAULT '1' COMMENT '练习模式：1=随机，2=章节专项，3=错题重做',
  `answer_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '学生答案',
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
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_run_record_id` (`run_record_id`),
  KEY `idx_practice_mode` (`practice_mode`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生自主练习记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

-- ------------------------------------------------------------
-- 数据库: javascene_teach
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_teach` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_teach`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_homework` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作业内容/描述',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `deadline` datetime NOT NULL COMMENT '截止时间',
  `allow_late_submit` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许迟交：0否 1是',
  `teacher_id` bigint NOT NULL COMMENT '发布教师ID',
  `class_id` bigint DEFAULT NULL COMMENT '发布班级ID（NULL表示全年级）',
  `grade` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级（class_id为空时表示全年级作业）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1=未发布，2=已发布，3=已截止，4=已归档）',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `close_time` datetime DEFAULT NULL COMMENT '关闭时间',
  `close_reason` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关闭原因',
  `total_score` int NOT NULL DEFAULT '0' COMMENT '作业总分',
  `question_count` int NOT NULL DEFAULT '0' COMMENT '题目数量',
  `submitted_count` int NOT NULL DEFAULT '0' COMMENT '提交数量',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_homework_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID',
  `question_id` bigint NOT NULL COMMENT '题目ID（关联 exam_question）',
  `source_type` tinyint DEFAULT NULL COMMENT '来源：1教材习题生成 2AI生成 3人工录入',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '题干快照（发布时固化）',
  `short_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简短题目快照（一句话概括）',
  `render_config` json DEFAULT NULL COMMENT '前端渲染配置快照（填空 blankCount、选择 choiceMode 等）',
  `type` tinyint DEFAULT NULL COMMENT '题型：1选择 2填空 3判断 4程序阅读 5编程 6简答',
  `options` json DEFAULT NULL COMMENT '选项快照（选择题用）',
  `answer` json DEFAULT NULL COMMENT '标准答案快照（教师可见，学生端不返回）',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '解析快照',
  `code_framework` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '编程题代码框架快照（发布时固化，学生答题编辑器直接用它初始化）',
  `requirements` json DEFAULT NULL COMMENT '作答要求快照（JSON数组，主观题/编程题）',
  `hints` json DEFAULT NULL COMMENT '阶梯提示快照（JSON数组，学生按需展开）',
  `solution_outline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '解题思路快照（做错后可展示）',
  `test_cases` json DEFAULT NULL COMMENT '测试用例快照（JSON数组，重做判题凭据，禁止下发学生端）',
  `judge_mode` tinyint DEFAULT NULL COMMENT '判题方式快照：1=AI批改 2=测试用例判题 3=混合',
  `difficulty` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '难度名称快照（如 简单/进阶/拔高）',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID快照',
  `knowledge_points` json DEFAULT NULL COMMENT '知识点快照',
  `default_score` int DEFAULT NULL COMMENT '题目默认分值（快照，用于score为空时兜底）',
  `page` int DEFAULT NULL COMMENT '教材页码（教材题快照）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业题目关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_ppt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名（含UUID）',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
  `md_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PPT原始Markdown内容',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PPT文件记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_ppt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '模板说明',
  `theme_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视觉主题编码（对应 CSS 文件名）',
  `design_tokens` json DEFAULT NULL COMMENT '设计令牌（颜色、字体等）',
  `structure_md` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容结构（MD 骨架）',
  `preview_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预览图URL',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统预设',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID（自定义模板）',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_is_system` (`is_system`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PPT模板表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_ppt_theme` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_code` varchar(64) NOT NULL COMMENT '主题编码，被 teach_ppt_template.theme_code 引用',
  `name` varchar(128) NOT NULL COMMENT '主题名称',
  `description` varchar(100) DEFAULT NULL COMMENT '主题外观描述',
  `css_text` mediumtext NOT NULL COMMENT '主题 CSS 全文',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号，更新时 +1，前端据此失效缓存',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0',
  `deleted` tinyint NOT NULL DEFAULT '0',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_theme_code` (`theme_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PPT主题';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `teach_prepare_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '章节ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `lesson_plan` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '教案内容（Markdown）',
  `key_points` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '重难点清单（Markdown）',
  `homework` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '课后习题（Markdown）',
  `lab` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '上机实训方案（Markdown）',
  `pptx_md` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'PPT原始Markdown内容',
  `pptx_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'PPT文件路径',
  `theme_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成PPT时使用的主题编码（补生成按此还原样式）',
  `status` tinyint DEFAULT '2' COMMENT '状态（1=成功，0=失败，2=生成中）',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备课记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- 种子数据: teach_ppt_template
USE `javascene_teach`;
INSERT IGNORE INTO `teach_ppt_template` (`id`, `name`, `description`, `theme_code`, `design_tokens`, `structure_md`, `preview_image`, `is_system`, `teacher_id`, `is_active`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES (1,'Java标准教学','目录→知识点→代码→总结，适合常规新授课','java-default',NULL,'# 第X章 章节名\r\n\r\n     Java程序设计基础\r\n\r\n     ---\r\n\r\n     # 本节课内容\r\n\r\n     1. 知识点1（P.xx）\r\n     2. 知识点2（P.xx）\r\n     3. 知识点3（P.xx）\r\n\r\n     ---\r\n\r\n     # 知识点1\r\n\r\n     - 定义：\r\n     - 要点1：\r\n     - 要点2：\r\n\r\n     ---\r\n\r\n     # 代码演示：示例标题\r\n\r\n     ```java\r\n     // 示例代码\r\n     ```\r\n\r\n     - 逐点讲解：\r\n     - 页脚：P.xx\r\n\r\n     ---\r\n\r\n     # 知识点2\r\n\r\n     - 定义：\r\n     - 要点1：\r\n\r\n     ---\r\n\r\n     # 本章小结\r\n\r\n     - 核心回顾\r\n     - 易错提醒',NULL,1,NULL,1,1,'2026-10-26 19:17:01','2026-10-26 19:17:01',0),(2,'Java案例驱动','案例→分析→实现→总结，适合实验课/案例课','java-uncover',NULL,'# 案例：XXX\r\n\r\n     ---\r\n\r\n     # 问题分析\r\n\r\n     - 问题描述：\r\n     - 涉及知识点：\r\n\r\n     ---\r\n\r\n     # 知识点讲解\r\n\r\n     - 核心概念：\r\n\r\n     ---\r\n\r\n     # 代码实现\r\n\r\n     ```java\r\n     // 实现代码\r\n     ```\r\n\r\n     ---\r\n\r\n     # 运行结果\r\n\r\n     - 结果分析：\r\n\r\n     ---\r\n\r\n     # 总结与拓展\r\n\r\n     - 一句话总结\r\n     - 拓展思考',NULL,1,NULL,1,2,'2026-10-26 19:17:01','2026-10-26 19:17:01',0),(3,'Java概念对比','概念A vs 概念B 对比表结构，适合易混淆知识点','java-minimal',NULL,'# 概念A vs 概念B\r\n\r\n     ---\r\n\r\n     # 概念A\r\n\r\n     - 定义：\r\n     - 特征：\r\n\r\n     ---\r\n\r\n     # 概念B\r\n\r\n     - 定义：\r\n     - 特征：\r\n\r\n     ---\r\n\r\n     # 对比表\r\n\r\n     | 对比项 | 概念A | 概念B |\r\n     |--------|-------|-------|\r\n     | 定义 | | |\r\n     | 用途 | | |\r\n     | 场景 | | |\r\n\r\n     ---\r\n\r\n     # 总结\r\n\r\n     - 一句话结论',NULL,1,NULL,1,3,'2026-10-26 19:17:01','2026-10-26 19:17:01',0);

-- 种子数据: teach_ppt_theme
USE `javascene_teach`;
INSERT IGNORE INTO `teach_ppt_theme` (`id`, `theme_code`, `name`, `description`, `css_text`, `version`, `is_active`, `sort_order`, `deleted`, `update_time`, `create_time`) VALUES (1,'java-default','Java标准教学','白底蓝字，清爽教学风','/* @theme java-default */\n\n/* Java标准教学主题：白底蓝字，清爽教学风 */\nsection {\n  padding: 60px;\n  background: #ffffff;\n  color: #333333;\n  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;\n  font-size: 26px;\n  line-height: 1.6;\n}\n\nh1 {\n  color: #1a56a0;\n  font-size: 46px;\n  font-weight: bold;\n  border-bottom: 3px solid #1a56a0;\n  padding-bottom: 16px;\n}\n\nh2 {\n  color: #1a56a0;\n  font-size: 36px;\n  font-weight: bold;\n}\n\nh3 {\n  color: #2c6cb0;\n  font-size: 30px;\n}\n\nul li, ol li {\n  margin-bottom: 10px;\n}\n\nstrong {\n  color: #c7254e;\n}\n\ncode {\n  background: #f0f4f8;\n  border-radius: 4px;\n  padding: 2px 8px;\n  color: #c7254e;\n  font-family: Consolas, \'Courier New\', monospace;\n}\n\npre {\n  background: #2d2d2d;\n  border-radius: 8px;\n  padding: 20px;\n  font-size: 20px;\n}\n\npre code {\n  background: none;\n  padding: 0;\n  color: #f8f8f2;\n}\n\ntable {\n  font-size: 22px;\n  border-collapse: collapse;\n}\n\nth {\n  background: #1a56a0;\n  color: #ffffff;\n  padding: 10px 16px;\n}\n\ntd {\n  border: 1px solid #d0d7de;\n  padding: 8px 16px;\n}\n\nblockquote {\n  border-left: 5px solid #1a56a0;\n  padding-left: 16px;\n  color: #666666;\n}\n',1,1,1,0,'2026-10-26 20:26:27','2026-10-26 20:26:27'),(2,'java-dark','Java暗色演示','黑底白字，投影环境高对比','/* @theme java-dark */\n\n/* Java暗色演示主题：黑底白字，投影环境高对比 */\nsection {\n  padding: 60px;\n  background: #1e1e1e;\n  color: #e8e8e8;\n  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;\n  font-size: 26px;\n  line-height: 1.6;\n}\n\nh1 {\n  color: #4fc3f7;\n  font-size: 46px;\n  font-weight: bold;\n  border-bottom: 3px solid #4fc3f7;\n  padding-bottom: 16px;\n}\n\nh2 {\n  color: #4fc3f7;\n  font-size: 36px;\n  font-weight: bold;\n}\n\nh3 {\n  color: #81d4fa;\n  font-size: 30px;\n}\n\nul li, ol li {\n  margin-bottom: 10px;\n}\n\nstrong {\n  color: #ffca28;\n}\n\ncode {\n  background: #2d2d2d;\n  border-radius: 4px;\n  padding: 2px 8px;\n  color: #ffca28;\n  font-family: Consolas, \'Courier New\', monospace;\n}\n\npre {\n  background: #252526;\n  border: 1px solid #3a3a3a;\n  border-radius: 8px;\n  padding: 20px;\n  font-size: 20px;\n}\n\npre code {\n  background: none;\n  padding: 0;\n  color: #d4d4d4;\n}\n\ntable {\n  font-size: 22px;\n  border-collapse: collapse;\n}\n\nth {\n  background: #4fc3f7;\n  color: #1e1e1e;\n  padding: 10px 16px;\n}\n\ntd {\n  border: 1px solid #3a3a3a;\n  padding: 8px 16px;\n}\n\nblockquote {\n  border-left: 5px solid #ffca28;\n  padding-left: 16px;\n  color: #9e9e9e;\n}\n',1,1,2,0,'2026-10-26 20:26:27','2026-10-26 20:26:27'),(3,'java-minimal','Java极简','大留白细字重，适合概念对比、严谨学术场合','/* @theme java-minimal */\n\n/* Java极简主题：大留白细字重，适合概念对比、严谨学术场合 */\nsection {\n  padding: 80px;\n  background: #fafafa;\n  color: #2c2c2c;\n  font-family: \'Noto Serif SC\', Georgia, \'Times New Roman\', serif;\n  font-size: 25px;\n  line-height: 1.7;\n}\n\nh1 {\n  color: #2c2c2c;\n  font-size: 48px;\n  font-weight: 300;\n  letter-spacing: 2px;\n  border-bottom: 1px solid #cccccc;\n  padding-bottom: 20px;\n}\n\nh2 {\n  color: #555555;\n  font-size: 34px;\n  font-weight: 400;\n}\n\nh3 {\n  color: #777777;\n  font-size: 28px;\n  font-weight: 400;\n}\n\nul li, ol li {\n  margin-bottom: 10px;\n}\n\nstrong {\n  color: #000000;\n  font-weight: bold;\n}\n\ncode {\n  background: #f0f0f0;\n  border-radius: 3px;\n  padding: 2px 8px;\n  color: #d63384;\n  font-family: Consolas, \'Courier New\', monospace;\n}\n\npre {\n  background: #ffffff;\n  border: 1px solid #dddddd;\n  border-radius: 4px;\n  padding: 20px;\n  font-size: 20px;\n}\n\npre code {\n  background: none;\n  padding: 0;\n  color: #333333;\n}\n\ntable {\n  font-size: 22px;\n  border-collapse: collapse;\n}\n\nth {\n  border-bottom: 2px solid #2c2c2c;\n  padding: 10px 16px;\n  font-weight: 400;\n}\n\ntd {\n  border-bottom: 1px solid #dddddd;\n  padding: 8px 16px;\n}\n\nblockquote {\n  border-left: 3px solid #999999;\n  padding-left: 16px;\n  color: #888888;\n}\n',1,1,3,0,'2026-10-26 20:26:27','2026-10-26 20:26:27'),(4,'java-uncover','Java活泼渐变','紫蓝渐变背景，适合案例驱动、互动课堂','/* @theme java-uncover */\n\n/* Java活泼渐变主题：紫蓝渐变背景，适合案例驱动、互动课堂 */\nsection {\n  padding: 70px;\n  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n  color: #ffffff;\n  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;\n  font-size: 27px;\n  line-height: 1.6;\n}\n\nh1 {\n  color: #ffd54f;\n  font-size: 48px;\n  font-weight: bold;\n  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);\n}\n\nh2 {\n  color: #ffe082;\n  font-size: 36px;\n  font-weight: bold;\n  border-left: 6px solid #ffd54f;\n  padding-left: 14px;\n}\n\nh3 {\n  color: #fff3e0;\n  font-size: 30px;\n}\n\nul li, ol li {\n  margin-bottom: 10px;\n}\n\nstrong {\n  color: #ffd54f;\n}\n\ncode {\n  background: rgba(255, 255, 255, 0.15);\n  border-radius: 4px;\n  padding: 2px 8px;\n  color: #ffe082;\n  font-family: Consolas, \'Courier New\', monospace;\n}\n\npre {\n  background: rgba(0, 0, 0, 0.35);\n  border-radius: 8px;\n  padding: 20px;\n  font-size: 20px;\n}\n\npre code {\n  background: none;\n  padding: 0;\n  color: #f8f8f2;\n}\n\ntable {\n  font-size: 22px;\n  border-collapse: collapse;\n}\n\nth {\n  background: #ffd54f;\n  color: #5e35b1;\n  padding: 10px 16px;\n}\n\ntd {\n  border: 1px solid rgba(255, 255, 255, 0.4);\n  padding: 8px 16px;\n}\n\nblockquote {\n  border-left: 5px solid #ffd54f;\n  padding-left: 16px;\n  color: #e0e0e0;\n}\n',1,1,4,0,'2026-10-26 20:26:27','2026-10-26 20:26:27');

-- ------------------------------------------------------------
-- 数据库: javascene_exam
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_exam` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_exam`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_grading_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID（关联 teach_homework）',
  `user_id` bigint NOT NULL COMMENT '提交学生ID',
  `total_score` int DEFAULT '100' COMMENT '作业满分',
  `student_score` int DEFAULT NULL COMMENT '学生总分',
  `score_detail` json DEFAULT NULL COMMENT '单题得分与错误原因，JSON数组',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '批改状态：0=待批改，1=已完成，2=批改失败',
  `error_message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '批改失败原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_user` (`homework_id`,`user_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_homework_id` (`homework_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业批改任务表（按学生+作业汇总）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_homework_submit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `homework_id` bigint NOT NULL COMMENT '作业ID（关联teach模块）',
  `user_id` bigint NOT NULL COMMENT '提交学生ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `snapshot_id` bigint NOT NULL DEFAULT '0' COMMENT '作业题目快照ID（teach_homework_question.id）；0=未上报（按题库版本归档）',
  `answer_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '学生答案（编程题为完整代码）',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作业提交表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `short_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简短题目（一句话概括，列表/卡片展示用）',
  `render_config` json DEFAULT NULL COMMENT '前端渲染配置（JSON对象）：填空 blankCount、选择 choiceMode 等',
  `type` tinyint NOT NULL COMMENT '题型（1=选择，2=填空，3=判断，4=程序阅读，5=编程题）',
  `options` json DEFAULT NULL COMMENT '选项（JSON数组，选择题用）',
  `requirements` json DEFAULT NULL COMMENT '主观题要求',
  `code_framework` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '编程题代码框架',
  `hints` json DEFAULT NULL COMMENT '阶梯提示，JSON数组',
  `related_question_ids` json DEFAULT NULL COMMENT '相似题ID列表',
  `answer` json NOT NULL COMMENT '标准答案',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '解析',
  `solution_outline` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '解题思路概述',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `difficulty_config_id` bigint DEFAULT NULL COMMENT '难度配置ID（逻辑外键 exam_question_difficulty_config.id）',
  `difficulty_score` decimal(3,1) DEFAULT NULL COMMENT '原始难度分（1.0-5.0）',
  `cognitive_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '认知层次',
  `ability_dimensions` json DEFAULT NULL COMMENT '能力维度',
  `common_mistakes` json DEFAULT NULL COMMENT '常见错误',
  `knowledge_points` json DEFAULT NULL,
  `score` int DEFAULT '5' COMMENT '默认分值',
  `weight` decimal(5,2) DEFAULT '1.00' COMMENT '组卷权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `test_cases` json DEFAULT NULL COMMENT '测试用例，JSON数组',
  `judge_mode` tinyint DEFAULT '1' COMMENT '判题方式：1=AI批改，2=测试用例判题，3=混合',
  `submit_count` int DEFAULT '0' COMMENT '总提交次数',
  `accepted_count` int DEFAULT '0' COMMENT '通过次数',
  `acceptance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '通过率（%）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_type` (`type`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_difficulty_config` (`difficulty_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_question_difficulty_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL COMMENT '难度编码，如 basic/medium/hard/custom_001',
  `name` varchar(64) NOT NULL COMMENT '难度名称，如 简单、中等、困难',
  `description` varchar(512) DEFAULT NULL COMMENT '难度说明',
  `example` varchar(512) DEFAULT NULL COMMENT '题目示例',
  `level_order` int NOT NULL DEFAULT '1' COMMENT '难度排序，1最简单',
  `cognitive_levels` json DEFAULT NULL COMMENT '认知层次，如 ["记忆","理解"]',
  `knowledge_point_min` int NOT NULL DEFAULT '1' COMMENT '最少知识点数量',
  `knowledge_point_max` int NOT NULL DEFAULT '1' COMMENT '最多知识点数量',
  `allow_cross_points` tinyint NOT NULL DEFAULT '0' COMMENT '是否允许跨知识点，0否1是',
  `reasoning_steps_min` int NOT NULL DEFAULT '0' COMMENT '最少推理步骤',
  `reasoning_steps_max` int NOT NULL DEFAULT '1' COMMENT '最多推理步骤',
  `code_lines_min` int NOT NULL DEFAULT '0' COMMENT '最少代码行数',
  `code_lines_max` int NOT NULL DEFAULT '5' COMMENT '最多代码行数',
  `question_length_max` int NOT NULL DEFAULT '80' COMMENT '题干最大字数',
  `option_count` int NOT NULL DEFAULT '4' COMMENT '选项数量',
  `distractor_strength` varchar(16) NOT NULL DEFAULT 'low' COMMENT '干扰项强度：low/medium/high',
  `score_min` decimal(4,2) NOT NULL DEFAULT '1.00' COMMENT '难度分下限',
  `score_max` decimal(4,2) NOT NULL DEFAULT '2.00' COMMENT '难度分上限',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统预设',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID，NULL表示系统级,关联user表',
  `chapter_id` bigint DEFAULT NULL COMMENT '绑定章节，NULL表示通用',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1启用 0禁用',
  `sort_order` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code_teacher_chapter` (`code`,`teacher_id`,`chapter_id`),
  KEY `idx_teacher_chapter` (`teacher_id`,`chapter_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='出题难度配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_question_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `snapshot_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '快照幂等键：H:{questionId}:{fingerprint}（题库版本） 或 P:{questionId}:{teachSnapshotId}（作业发布快照）',
  `snapshot_type` tinyint NOT NULL DEFAULT '1' COMMENT '快照来源：1=题库版本（按内容指纹去重），2=作业发布快照（按 teach 快照ID去重）',
  `question_id` bigint NOT NULL COMMENT '题库题目ID（exam_question.id）',
  `teach_snapshot_id` bigint NOT NULL DEFAULT '0' COMMENT '作业题目快照ID（teach_homework_question.id）；题库来源为 0',
  `fingerprint` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '内容指纹：题干+选项+标准答案的 MD5，用于判断题库题目是否已修订',
  `short_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简短题目快照',
  `question_type` tinyint DEFAULT NULL COMMENT '题型快照：1选择 2填空 3判断 4程序阅读 5编程 6简答',
  `score` int DEFAULT NULL COMMENT '题目分值快照',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID快照',
  `difficulty_config_id` bigint DEFAULT NULL COMMENT '难度配置ID快照',
  `snapshot_view` json DEFAULT NULL COMMENT '学生可见渲染包：content/shortTitle/type/options/renderConfig/codeFramework/requirements/hints/score/chapterId/knowledgePoints',
  `snapshot_judge` json DEFAULT NULL COMMENT '判题与解析包：answer/analysis/solutionOutline/testCases/judgeMode。禁止下发学生端',
  `legacy` tinyint NOT NULL DEFAULT '0' COMMENT '1=历史迁移数据，题面按迁移时题库回填，可能已非当时版本',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_key` (`snapshot_key`),
  KEY `idx_snapshot_question` (`question_id`),
  KEY `idx_snapshot_type_question` (`snapshot_type`,`question_id`),
  KEY `idx_snapshot_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目快照表（一个题目版本一行，错题本/重做共用，跨学生复用）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `exam_wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `snapshot_id` bigint NOT NULL DEFAULT '0' COMMENT '题目快照ID（exam_question_snapshot.id）',
  `source` tinyint DEFAULT '1' COMMENT '来源：1=作业，2=刷题',
  `wrong_count` int NOT NULL DEFAULT '0' COMMENT '累计错误次数',
  `last_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '最近一次错误答案',
  `mastered` tinyint NOT NULL DEFAULT '0' COMMENT '是否已掌握：0=未掌握，1=已掌握',
  `last_wrong_time` datetime DEFAULT NULL COMMENT '最近答错时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_snapshot_source` (`user_id`,`snapshot_id`,`source`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_wrong_user_time` (`user_id`,`last_wrong_time`),
  KEY `idx_wrong_snapshot` (`snapshot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题归档表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- 种子数据: exam_question_difficulty_config
USE `javascene_exam`;
INSERT IGNORE INTO `exam_question_difficulty_config` (`id`, `code`, `name`, `description`, `example`, `level_order`, `cognitive_levels`, `knowledge_point_min`, `knowledge_point_max`, `allow_cross_points`, `reasoning_steps_min`, `reasoning_steps_max`, `code_lines_min`, `code_lines_max`, `question_length_max`, `option_count`, `distractor_strength`, `score_min`, `score_max`, `is_system`, `teacher_id`, `chapter_id`, `status`, `sort_order`, `create_time`, `update_time`, `deleted`) VALUES (1,'basic','简单','考查基础概念','下列关于类的说法正确的是？',1,'[\"记忆\", \"理解\"]',1,1,0,0,1,0,5,80,4,'low',1.00,2.00,1,NULL,NULL,1,1,'2026-09-24 17:44:13','2026-09-24 17:44:13',0),(2,'intermediate','进阶','考查概念应用','编写方法实现两个整数交换后返回',2,'[\"理解\", \"应用\"]',1,2,1,1,3,3,15,150,4,'medium',2.00,4.00,1,NULL,NULL,1,2,'2026-09-24 17:44:13','2026-09-24 17:44:13',0),(3,'advanced','拔高','考查综合设计','设计一个包含继承与多态的类层次',3,'[\"分析\", \"评价\"]',2,3,1,3,6,15,40,300,5,'high',4.00,6.00,1,NULL,NULL,1,3,'2026-09-24 17:44:13','2026-09-24 17:44:13',0);

-- ------------------------------------------------------------
-- 数据库: javascene_knowledge
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_knowledge` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_knowledge`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_chapter` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `course_id` bigint DEFAULT NULL COMMENT '所属课程ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父章节ID（0表示顶级章）',
  `chapter_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节编号（如 3.2）',
  `top_chapter_code` bigint DEFAULT NULL COMMENT '所属大章',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '章节标题',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '章节简介',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_delete_flag` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材章节表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_code_snippet` (
  `id` bigint NOT NULL COMMENT '主键ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材代码片段表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_concept` (
  `id` bigint NOT NULL COMMENT '主键ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材核心概念表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_content` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `knowledge_point_id` bigint DEFAULT NULL COMMENT '关联知识点ID（可为空）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教材原文摘录',
  `content_type` tinyint DEFAULT '1' COMMENT '类型（1=概念定义，2=例题，3=代码案例，4=习题）',
  `page` int DEFAULT NULL COMMENT '页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `dify_document_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify知识库文档ID（关联外部）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_knowledge_point_id` (`knowledge_point_id`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_deleted` (`deleted`),
  FULLTEXT KEY `ft_content` (`content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材原文内容表（AI检索用）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_difficult_point` (
  `id` bigint NOT NULL COMMENT '主键ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材重难点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_exercise` (
  `id` bigint NOT NULL COMMENT '主键ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教材课后习题表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_keyword` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '关联章节ID（NULL表示全局关键词）',
  `knowledge_point_id` bigint DEFAULT NULL COMMENT '关联知识点ID（可为空）',
  `keyword` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关键词（如 继承、多态）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关键词释义/说明',
  `page` int DEFAULT NULL COMMENT '教材页码',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `synonyms` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '同义词（逗号分隔，如：封装,信息隐藏）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_point_id` (`knowledge_point_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材关键词表（AI校验用）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_point` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '知识点描述/定义（从教材摘录）',
  `difficulty` tinyint DEFAULT '1' COMMENT '难度（1=了解，2=理解，3=掌握，4=重难点）',
  `keyword` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关键词（逗号分隔，如：封装,信息隐藏）',
  `page_ref` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '教材页码（如 P.123-125）',
  `page_order` int NOT NULL DEFAULT '0' COMMENT '同一页内顺序（数字越小越靠前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0=未删，1=已删）',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_deleted` (`deleted`),
  FULLTEXT KEY `ft_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教材知识点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_upload_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dataset_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Dify知识库ID',
  `document_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify文档ID（上传成功后填充）',
  `batch` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify批次标识（用于查询索引状态）',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `chapter_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '章节名称（冗余，便于查询）',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小（字节）',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型：txt/md/pdf/docx',
  `upload_type` tinyint NOT NULL DEFAULT '1' COMMENT '上传类型：1=新增，2=更新',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0=处理中，1=成功，2=失败',
  `total_segments` int DEFAULT '0' COMMENT '生成的分段总数',
  `completed_segments` int DEFAULT '0' COMMENT '已完成的分段数',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因',
  `upload_by` bigint NOT NULL COMMENT '上传人ID（关联 sys_user.id）',
  `upload_by_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上传人姓名（冗余）',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `finish_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_dataset_id` (`dataset_id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_upload_by` (`upload_by`),
  KEY `idx_status` (`status`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库上传记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `knowledge_weight_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `chapter_id` bigint NOT NULL COMMENT '关联章节ID',
  `knowledge_point_id` bigint NOT NULL COMMENT '关联知识点ID',
  `weight` decimal(5,2) NOT NULL DEFAULT '1.00' COMMENT '权重值（1.00-5.00）',
  `base_question_count` int DEFAULT '1' COMMENT '基础出题数量',
  `max_question_count` int DEFAULT '5' COMMENT '最大出题数量',
  `difficulty_distribution` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '30,50,20' COMMENT '难度分布百分比',
  `is_active` tinyint DEFAULT '1' COMMENT '是否启用（1=启用，0=禁用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chapter_point` (`chapter_id`,`knowledge_point_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_weight` (`weight` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点权重配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- ------------------------------------------------------------
-- 数据库: javascene_agent_qa
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_agent_qa` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_agent_qa`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `qa_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `chapter_id` bigint DEFAULT NULL COMMENT '章节ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID（一次完整对话的唯一标识）',
  `round_no` int NOT NULL DEFAULT '1' COMMENT '轮次序号（同一session内从1递增）',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学生问题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI回答',
  `knowledge_points` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联知识点（逗号分隔）',
  `reference_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '教材原文引用',
  `is_textbook_match` tinyint DEFAULT '1' COMMENT '是否匹配教材：1=是，0=否（超纲）',
  `dify_message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dify消息ID',
  `tokens_used` int DEFAULT '0' COMMENT '消耗token数',
  `response_time` int DEFAULT '0' COMMENT '响应耗时（毫秒）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_chapter_id` (`chapter_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_session_round` (`session_id`,`round_no`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生问答记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

-- ------------------------------------------------------------
-- 数据库: javascene_dialogue
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `javascene_dialogue` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE `javascene_dialogue`;

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `intent_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `draft_intent_type` varchar(32) NOT NULL COMMENT 'AI推测的意图类型',
  `draft_template_text` json NOT NULL COMMENT 'AI生成的模板问法',
  `original_user_query` varchar(500) NOT NULL COMMENT '触发该建议的用户原始问句',
  `status` tinyint DEFAULT '0' COMMENT '0-待审核, 1-已批准(已入库), 2-已拒绝',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `reviewed_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

