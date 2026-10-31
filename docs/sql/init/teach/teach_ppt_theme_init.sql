-- ============================================================
-- teach_ppt_theme 主题初始化数据（4 套）
-- 数据来源：teach/src/main/resources/marp-themes/ 下的 CSS 原文，
--           css_text 与文件逐字一致（含首行 @theme 声明，Marp 靠它注册主题名）
-- 说明：表结构已由 table.sql 建立；本脚本只插数据，可重复执行（先删后插）
-- 排序：sort_order 越小越靠前，java-default 作为默认主题
-- ============================================================

DELETE FROM `teach_ppt_theme` WHERE `theme_code` IN ('java-default', 'java-dark', 'java-minimal', 'java-uncover');

INSERT INTO `teach_ppt_theme`
(`theme_code`, `name`, `description`, `css_text`, `version`, `is_active`, `sort_order`, `deleted`)
VALUES
('java-default', 'Java标准教学', '白底蓝字，清爽教学风',
'/* @theme java-default */

/* Java标准教学主题：白底蓝字，清爽教学风 */
section {
  padding: 60px;
  background: #ffffff;
  color: #333333;
  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;
  font-size: 26px;
  line-height: 1.6;
}

h1 {
  color: #1a56a0;
  font-size: 46px;
  font-weight: bold;
  border-bottom: 3px solid #1a56a0;
  padding-bottom: 16px;
}

h2 {
  color: #1a56a0;
  font-size: 36px;
  font-weight: bold;
}

h3 {
  color: #2c6cb0;
  font-size: 30px;
}

ul li, ol li {
  margin-bottom: 10px;
}

strong {
  color: #c7254e;
}

code {
  background: #f0f4f8;
  border-radius: 4px;
  padding: 2px 8px;
  color: #c7254e;
  font-family: Consolas, \'Courier New\', monospace;
}

pre {
  background: #2d2d2d;
  border-radius: 8px;
  padding: 20px;
  font-size: 20px;
}

pre code {
  background: none;
  padding: 0;
  color: #f8f8f2;
}

table {
  font-size: 22px;
  border-collapse: collapse;
}

th {
  background: #1a56a0;
  color: #ffffff;
  padding: 10px 16px;
}

td {
  border: 1px solid #d0d7de;
  padding: 8px 16px;
}

blockquote {
  border-left: 5px solid #1a56a0;
  padding-left: 16px;
  color: #666666;
}
',
1, 1, 1, 0),
('java-dark', 'Java暗色演示', '黑底白字，投影环境高对比',
'/* @theme java-dark */

/* Java暗色演示主题：黑底白字，投影环境高对比 */
section {
  padding: 60px;
  background: #1e1e1e;
  color: #e8e8e8;
  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;
  font-size: 26px;
  line-height: 1.6;
}

h1 {
  color: #4fc3f7;
  font-size: 46px;
  font-weight: bold;
  border-bottom: 3px solid #4fc3f7;
  padding-bottom: 16px;
}

h2 {
  color: #4fc3f7;
  font-size: 36px;
  font-weight: bold;
}

h3 {
  color: #81d4fa;
  font-size: 30px;
}

ul li, ol li {
  margin-bottom: 10px;
}

strong {
  color: #ffca28;
}

code {
  background: #2d2d2d;
  border-radius: 4px;
  padding: 2px 8px;
  color: #ffca28;
  font-family: Consolas, \'Courier New\', monospace;
}

pre {
  background: #252526;
  border: 1px solid #3a3a3a;
  border-radius: 8px;
  padding: 20px;
  font-size: 20px;
}

pre code {
  background: none;
  padding: 0;
  color: #d4d4d4;
}

table {
  font-size: 22px;
  border-collapse: collapse;
}

th {
  background: #4fc3f7;
  color: #1e1e1e;
  padding: 10px 16px;
}

td {
  border: 1px solid #3a3a3a;
  padding: 8px 16px;
}

blockquote {
  border-left: 5px solid #ffca28;
  padding-left: 16px;
  color: #9e9e9e;
}
',
1, 1, 2, 0),
('java-minimal', 'Java极简', '大留白细字重，适合概念对比、严谨学术场合',
'/* @theme java-minimal */

/* Java极简主题：大留白细字重，适合概念对比、严谨学术场合 */
section {
  padding: 80px;
  background: #fafafa;
  color: #2c2c2c;
  font-family: \'Noto Serif SC\', Georgia, \'Times New Roman\', serif;
  font-size: 25px;
  line-height: 1.7;
}

h1 {
  color: #2c2c2c;
  font-size: 48px;
  font-weight: 300;
  letter-spacing: 2px;
  border-bottom: 1px solid #cccccc;
  padding-bottom: 20px;
}

h2 {
  color: #555555;
  font-size: 34px;
  font-weight: 400;
}

h3 {
  color: #777777;
  font-size: 28px;
  font-weight: 400;
}

ul li, ol li {
  margin-bottom: 10px;
}

strong {
  color: #000000;
  font-weight: bold;
}

code {
  background: #f0f0f0;
  border-radius: 3px;
  padding: 2px 8px;
  color: #d63384;
  font-family: Consolas, \'Courier New\', monospace;
}

pre {
  background: #ffffff;
  border: 1px solid #dddddd;
  border-radius: 4px;
  padding: 20px;
  font-size: 20px;
}

pre code {
  background: none;
  padding: 0;
  color: #333333;
}

table {
  font-size: 22px;
  border-collapse: collapse;
}

th {
  border-bottom: 2px solid #2c2c2c;
  padding: 10px 16px;
  font-weight: 400;
}

td {
  border-bottom: 1px solid #dddddd;
  padding: 8px 16px;
}

blockquote {
  border-left: 3px solid #999999;
  padding-left: 16px;
  color: #888888;
}
',
1, 1, 3, 0),
('java-uncover', 'Java活泼渐变', '紫蓝渐变背景，适合案例驱动、互动课堂',
'/* @theme java-uncover */

/* Java活泼渐变主题：紫蓝渐变背景，适合案例驱动、互动课堂 */
section {
  padding: 70px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  font-family: \'Microsoft YaHei\', \'PingFang SC\', \'Helvetica Neue\', sans-serif;
  font-size: 27px;
  line-height: 1.6;
}

h1 {
  color: #ffd54f;
  font-size: 48px;
  font-weight: bold;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

h2 {
  color: #ffe082;
  font-size: 36px;
  font-weight: bold;
  border-left: 6px solid #ffd54f;
  padding-left: 14px;
}

h3 {
  color: #fff3e0;
  font-size: 30px;
}

ul li, ol li {
  margin-bottom: 10px;
}

strong {
  color: #ffd54f;
}

code {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  padding: 2px 8px;
  color: #ffe082;
  font-family: Consolas, \'Courier New\', monospace;
}

pre {
  background: rgba(0, 0, 0, 0.35);
  border-radius: 8px;
  padding: 20px;
  font-size: 20px;
}

pre code {
  background: none;
  padding: 0;
  color: #f8f8f2;
}

table {
  font-size: 22px;
  border-collapse: collapse;
}

th {
  background: #ffd54f;
  color: #5e35b1;
  padding: 10px 16px;
}

td {
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 8px 16px;
}

blockquote {
  border-left: 5px solid #ffd54f;
  padding-left: 16px;
  color: #e0e0e0;
}
',
1, 1, 4, 0);
