-- ============================================================
-- teach_ppt_template 系统模板初始化数据（3 套）
-- 依据：《26.10.4.1作业发布与备课成果应用开发文档》5.8 节
-- 说明：表结构已由 table.sql 建立（idx_teacher_id / idx_is_system 索引已含）；
--       本脚本仅插入 3 套系统预设模板（is_system=1），干净库可重复执行前请先
--       DELETE FROM teach_ppt_template WHERE is_system = 1;
-- 执行人：sql 负责人（本脚本由 teach 模块负责人交付）
-- ============================================================

DELETE FROM `teach_ppt_template` WHERE `is_system` = 1;

INSERT INTO `teach_ppt_template`
(`name`, `description`, `theme_code`, `structure_md`, `preview_image`, `is_system`, `is_active`, `sort_order`, `deleted`)
VALUES
('Java标准教学', '目录→知识点→代码→总结，适合常规新授课', 'java-default',
'# 第X章 章节名

Java程序设计基础

---

# 本节课内容

1. 知识点1（P.xx）
2. 知识点2（P.xx）
3. 知识点3（P.xx）

---

# 知识点1

- 定义：
- 要点1：
- 要点2：

---

# 代码演示：示例标题

```java
// 示例代码
```

- 逐点讲解：
- 页脚：P.xx

---

# 知识点2

- 定义：
- 要点1：

---

# 本章小结

- 核心回顾
- 易错提醒',
NULL, 1, 1, 1, 0),

('Java案例驱动', '案例→分析→实现→总结，适合实验课/案例课', 'java-uncover',
'# 案例：XXX

---

# 问题分析

- 问题描述：
- 涉及知识点：

---

# 知识点讲解

- 核心概念：

---

# 代码实现

```java
// 实现代码
```

---

# 运行结果

- 结果分析：

---

# 总结与拓展

- 一句话总结
- 拓展思考',
NULL, 1, 1, 2, 0),

('Java概念对比', '概念A vs 概念B 对比表结构，适合易混淆知识点', 'java-minimal',
'# 概念A vs 概念B

---

# 概念A

- 定义：
- 特征：

---

# 概念B

- 定义：
- 特征：

---

# 对比表

| 对比项 | 概念A | 概念B |
|--------|-------|-------|
| 定义 | | |
| 用途 | | |
| 场景 | | |

---

# 总结

- 一句话结论',
NULL, 1, 1, 3, 0);
