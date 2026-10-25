@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ========== 这里改成你自己数据库配置 ==========
set DB_HOST=127.0.0.1
set DB_USER=root
set DB_PASS=123456
set DB_NAME=java_scene
set MYSQL_CMD=mysql
:: 如果mysql不是环境变量，这里写全路径，例如 set MYSQL_CMD="D:\mysql8\bin\mysql.exe"
:: ==============================================

echo ======================================
echo 开始批量执行SQL脚本
echo 数据库：%DB_NAME%
echo ======================================

:: 1. 先执行根目录 table.sql（建表，优先）
echo [1/11] 执行 table.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < table.sql
if !errorlevel! neq 0 (
    echo 【失败】table.sql执行出错！
    pause
    exit /b 1
)

:: 2. sys
echo [2/11] 执行 init/sys/sys.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/sys/sys.sql
if !errorlevel! neq 0 (
    echo 【失败】sys.sql执行出错！
    pause
    exit /b 1
)

::3. teach
echo [3/11] 执行 init/teach/teach_ppt_template_init.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/teach/teach_ppt_template_init.sql
if !errorlevel! neq 0 (
    echo 【失败】teach脚本执行出错！
    pause
    exit /b 1
)

::4. knowledge章节（按文件名顺序执行）
echo [4/11] 执行 init/konwledge/第1章Java语言概述.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第1章Java语言概述.sql
echo [5/11] 执行 init/konwledge/第2章Java编程基础.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第2章Java编程基础.sql
echo [6/11] 执行 init/konwledge/第3章控制结构.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第3章控制结构.sql
echo [7/11] 执行 init/konwledge/第4章方法.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第4章方法.sql
echo [8/11] 执行 init/konwledge/第5章数组、字符串.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第5章数组、字符串.sql
echo [9/11] 执行 init/konwledge/第6章面向对象程序设计（上）.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第6章面向对象程序设计（上）.sql
echo [10/11] 执行 init/konwledge/第7章面向对象程序设计（下）.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第7章面向对象程序设计（下）.sql
echo [11/11] 执行 init/konwledge/第8章内嵌类与lambda表达式.sql
%MYSQL_CMD% -h%DB_HOST% -u%DB_USER% -p%DB_PASS% %DB_NAME% < init/konwledge/第8章内嵌类与lambda表达式.sql

echo.
echo ✅ 全部脚本执行完成！
pause