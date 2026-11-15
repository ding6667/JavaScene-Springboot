-- 测试数据：2 个 2024 级班级 + 40 个学生账号（每班 20 人），班主任为 teacher_t
-- 密码列直接复用 admin 的 BCrypt 哈希，明文为 123456
-- 仅限本地开发库 javascene 执行；角色取自 sys_user.role_id（1=STUDENT），sys_user_role 未被代码使用

SET @teacher_id = (SELECT id FROM sys_user WHERE username = 'teacher_t');
SET @pwd = '$2a$10$WFQ35b7jGdXh.9WZjTqboey18lTaINsB5h1O2K9ySt3g1NIYnTwKm';

START TRANSACTION;

INSERT INTO sys_class (id, class_name, teacher_id, grade, student_count, course_id, deleted) VALUES
(1, '2024级软件工程1班', @teacher_id, '2024级', 20, NULL, 0),
(2, '2024级软件工程2班', @teacher_id, '2024级', 20, NULL, 0);

-- username = student_no（与 StudentInfoServiceImpl.importOne 的约定一致）
INSERT INTO sys_user (id, username, password, real_name, enabled, deleted, role_id) VALUES
(7,  '20240101', @pwd, '张伟', 1, 0, 1),
(8,  '20240102', @pwd, '王芳', 1, 0, 1),
(9,  '20240103', @pwd, '李娜', 1, 0, 1),
(10, '20240104', @pwd, '刘洋', 1, 0, 1),
(11, '20240105', @pwd, '陈静', 1, 0, 1),
(12, '20240106', @pwd, '杨帆', 1, 0, 1),
(13, '20240107', @pwd, '赵磊', 1, 0, 1),
(14, '20240108', @pwd, '黄敏', 1, 0, 1),
(15, '20240109', @pwd, '周杰', 1, 0, 1),
(16, '20240110', @pwd, '吴倩', 1, 0, 1),
(17, '20240111', @pwd, '徐强', 1, 0, 1),
(18, '20240112', @pwd, '孙丽', 1, 0, 1),
(19, '20240113', @pwd, '马超', 1, 0, 1),
(20, '20240114', @pwd, '朱婷', 1, 0, 1),
(21, '20240115', @pwd, '胡军', 1, 0, 1),
(22, '20240116', @pwd, '郭涛', 1, 0, 1),
(23, '20240117', @pwd, '林峰', 1, 0, 1),
(24, '20240118', @pwd, '何雪', 1, 0, 1),
(25, '20240119', @pwd, '高翔', 1, 0, 1),
(26, '20240120', @pwd, '罗晴', 1, 0, 1),
(27, '20240201', @pwd, '郑阳', 1, 0, 1),
(28, '20240202', @pwd, '梁宇', 1, 0, 1),
(29, '20240203', @pwd, '宋佳', 1, 0, 1),
(30, '20240204', @pwd, '唐龙', 1, 0, 1),
(31, '20240205', @pwd, '许静', 1, 0, 1),
(32, '20240206', @pwd, '韩磊', 1, 0, 1),
(33, '20240207', @pwd, '冯波', 1, 0, 1),
(34, '20240208', @pwd, '邓超', 1, 0, 1),
(35, '20240209', @pwd, '曹阳', 1, 0, 1),
(36, '20240210', @pwd, '彭悦', 1, 0, 1),
(37, '20240211', @pwd, '曾诚', 1, 0, 1),
(38, '20240212', @pwd, '李雨泽', 1, 0, 1),
(39, '20240213', @pwd, '王梓涵', 1, 0, 1),
(40, '20240214', @pwd, '张明', 1, 0, 1),
(41, '20240215', @pwd, '刘思远', 1, 0, 1),
(42, '20240216', @pwd, '陈嘉怡', 1, 0, 1),
(43, '20240217', @pwd, '杨睿', 1, 0, 1),
(44, '20240218', @pwd, '赵子墨', 1, 0, 1),
(45, '20240219', @pwd, '黄一鸣', 1, 0, 1),
(46, '20240220', @pwd, '吴桐', 1, 0, 1);

-- 班级归属由学号第 5-6 位（01/02）决定，class_name、grade 按应用自身写法从 sys_class 冗余复制
INSERT INTO javascene.sys_student_info
    (user_id, student_no, class_id, class_name, grade, major, college, enroll_year, graduation_year, deleted)
SELECT u.id,
       u.username,
       c.id,
       c.class_name,
       c.grade,
       '软件工程',
       '计算机学院',
       2024,
       2028,
       0
FROM javascene.sys_user u
         JOIN javascene.sys_class c
              ON c.id = CAST(SUBSTRING(u.username, 5, 2) AS UNSIGNED)
WHERE u.role_id = 1
  AND u.deleted = 0
  AND u.username REGEXP '^20240[12][0-9]{2}$'
ORDER BY u.username;

COMMIT;
