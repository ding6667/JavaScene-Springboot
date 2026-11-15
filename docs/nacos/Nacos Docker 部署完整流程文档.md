# Nacos Docker 部署完整流程文档

> 适用环境：Windows + Docker Desktop + PowerShell  
> 目标：Nacos v2.4.3 单机模式，数据持久化到 Docker MySQL  
> 核心问题：解决容器间通过容器名通信、端口冲突、数据库初始化

---

## 📋 前置条件

- Windows 已安装 Docker Desktop，且 Docker 正常运行
- PowerShell 可用
- 本机已安装 MySQL 9.3（路径 `E:\MtSql\bin\`），用于导出旧数据（如需要迁移）
- 已下载 Nacos 2.4.3 建表脚本 `E:\nacos-schema.sql`

---

## 一、解决本机 MySQL 与 Docker MySQL 的端口冲突

本机 MySQL93 服务默认占用 `3306`，Docker MySQL 也映射 `3306`，两者不能同时运行。部署 Docker MySQL 前，先停掉本机服务：

```powershell
net stop mysql93
```

> 后续如果需要用本机 MySQL 导出数据，再临时 `net start mysql93`，用完再停。

---

## 二、创建 Docker 网络（关键步骤）

Docker 默认的 `bridge` 网络**不支持容器名 DNS 解析**，会导致 Nacos 报 `UnknownHostException: mysql-shared`。必须先创建自定义网络：

```powershell
docker network create nacos-net
```

验证：

```powershell
docker network ls
```

应能看到 `nacos-net`。

---

## 三、部署 MySQL 容器

### 3.1 启动 MySQL

```powershell
docker run -d --name mysql-shared --network nacos-net --restart unless-stopped -e MYSQL_ROOT_PASSWORD=123456 -e TZ=Asia/Shanghai -p 3306:3306 -v mysql-data:/var/lib/mysql --health-cmd="mysqladmin ping -uroot -p123456" --health-interval=10s --health-timeout=5s --health-retries=12 mysql:9.3 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

关键参数说明：

| 参数 | 作用 |
|---|---|
| `--network nacos-net` | 加入自定义网络，支持容器名解析 |
| `-p 3306:3306` | 宿主机 3306 → 容器 3306，供 Windows 工具连接 |
| `-v mysql-data:/var/lib/mysql` | 数据持久化到 Docker 卷 |
| `--health-cmd` | 健康检查，供后续 Compose 编排使用 |
| `--character-set-server=utf8mb4` | 避免中文乱码 |

### 3.2 验证 MySQL 状态

```powershell
docker ps --filter "name=mysql-shared"
```

状态应为 `Up ... (healthy)`。

查看日志确认无报错：

```powershell
docker logs mysql-shared
```

### 3.3 从 Windows 连接 MySQL（可选验证）

```powershell
mysql -h 127.0.0.1 -P 3306 -u root -p
```

密码 `123456`。或用 Navicat / DBeaver 连接 `127.0.0.1:3306`。

---

## 四、初始化 Nacos 数据库

### 4.1 创建 `nacos_config` 库

```powershell
docker exec -it mysql-shared mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS nacos_config CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 4.2 获取 Nacos 2.4.3 建表脚本

如果本地还没有 `E:\nacos-schema.sql`，通过以下任一方式下载。

**方式 A：GitHub 代理加速（推荐）**

```powershell
Invoke-WebRequest -Uri "https://ghfast.top/https://raw.githubusercontent.com/alibaba/nacos/2.4.3/distribution/conf/mysql-schema.sql" -OutFile E:\nacos-schema.sql
```

**方式 B：Gitee 镜像**

```powershell
Invoke-WebRequest -Uri "https://gitee.com/dromara/dante-cloud/raw/v4.0.2.0-M3/configurations/scripts/nacos/sqls/nacos-mysql_2.4.3.sql" -OutFile E:\nacos-schema.sql
```

**方式 C：从镜像中提取（最保底）**

```powershell
docker create --name nacos-temp nacos/nacos-server:v2.4.3
docker cp nacos-temp:/home/nacos/conf/mysql-schema.sql E:\nacos-schema.sql
docker rm nacos-temp
```

**验证脚本内容：**

```powershell
Select-String -Path E:\nacos-schema.sql -Pattern "CREATE TABLE" | Select-Object -First 5
```

应能看到 `config_info`、`users` 等表的创建语句。

### 4.3 导入建表脚本

```powershell
cmd /c "docker exec -i mysql-shared mysql -uroot -p123456 nacos_config < E:\nacos-schema.sql"
```

### 4.4 验证表已创建

```powershell
docker exec -it mysql-shared mysql -uroot -p123456 -e "USE nacos_config; SHOW TABLES;"
```

应看到：

```
config_info
config_info_beta
config_info_tag
config_tags_relation
group_capacity
his_config_info
tenant_info
users
roles
permissions
```

再确认 `users` 表有默认账号：

```powershell
docker exec -it mysql-shared mysql -uroot -p123456 -e "USE nacos_config; SELECT username FROM users;"
```

应返回 `nacos`。

---

## 五、（可选）从本机 MySQL 迁移旧数据

> 仅当本机 MySQL93 中已有 `nacos_config` 存量数据时执行。

### 5.1 启动本机 MySQL93

```powershell
net start mysql93
```

### 5.2 导出旧数据

```powershell
E:\MtSql\bin\mysqldump.exe -u root -p --default-character-set=utf8mb4 --databases nacos_config --routines --triggers --events --result-file=E:\dump.sql
```

> 用 `--result-file` 而非 `>` 重定向，避免 PowerShell 写 UTF-16 导致导入报错。

### 5.3 停掉本机 MySQL93（释放 3306）

```powershell
net stop mysql93
```

### 5.4 导入到 Docker MySQL

```powershell
cmd /c "docker exec -i mysql-shared mysql -uroot -p123456 < E:\dump.sql"
```

> dump.sql 自带 `CREATE DATABASE`，无需手动建库。若导出时没加 `--databases`，需先在命令末尾指定库名 `nacos_config`。

### 5.5 验证数据

```powershell
docker exec -it mysql-shared mysql -uroot -p123456 -e "USE nacos_config; SELECT COUNT(*) FROM config_info;"
```

---

## 六、部署 Nacos 容器

### 6.1 启动命令

```powershell
docker run -d --name nacos-standalone --network nacos-net -p 8848:8848 -p 9848:9848 -e MODE=standalone -e SPRING_DATASOURCE_PLATFORM=mysql -e MYSQL_SERVICE_HOST=mysql-shared -e MYSQL_SERVICE_PORT=3306 -e MYSQL_SERVICE_DB_NAME=nacos_config -e MYSQL_SERVICE_USER=root -e MYSQL_SERVICE_PASSWORD=123456 -e MYSQL_SERVICE_DB_PARAM="characterEncoding=utf8&connectTimeout=5000&socketTimeout=5000&useSSL=false&allowPublicKeyRetrieval=true" nacos/nacos-server:v2.4.3
```

### 6.2 参数说明

| 参数 | 说明 |
|---|---|
| `--network nacos-net` | **必须**，与 MySQL 同网络，才能解析 `mysql-shared` |
| `-p 8848:8848` | 控制台 / HTTP API 端口 |
| `-p 9848:9848` | Nacos 2.x gRPC 端口，客户端必需 |
| `MODE=standalone` | 单机模式 |
| `SPRING_DATASOURCE_PLATFORM=mysql` | 使用外部 MySQL，而非内嵌 Derby |
| `MYSQL_SERVICE_HOST=mysql-shared` | 用容器名而非 IP，IP 变化不受影响 |
| `MYSQL_SERVICE_DB_PARAM` | 包含 `allowPublicKeyRetrieval=true`，兼容 MySQL 8/9 |

### 6.3 验证容器状态

```powershell
docker ps --filter "name=nacos-standalone"
```

状态应为 `Up`，不能是 `Exited`。

### 6.4 查看启动日志

```powershell
docker logs -f nacos-standalone
```

关键成功标志：

```
Nacos started successfully in stand alone mode. use external storage
```

出现 `use external storage` 表示已成功连接 MySQL（而非内嵌数据库）。

按 `Ctrl+C` 退出日志跟踪（不会停止容器）。

---

## 七、访问 Nacos 控制台

浏览器打开：

```
http://localhost:8848/nacos
```

- 默认用户名：`nacos`
- 默认密码：`nacos`

登录后可在「配置管理」中新建测试配置，然后到 MySQL 验证是否写入：

```powershell
docker exec -it mysql-shared mysql -uroot -p123456 -e "USE nacos_config; SELECT data_id, group_id FROM config_info;"
```

---

## 八、常见问题排查

### 问题 1：`UnknownHostException: mysql-shared`

**原因**：Nacos 和 MySQL 不在同一自定义网络，Docker 无法解析容器名。

**修复**：

```powershell
# 确认两个容器都在 nacos-net
docker network inspect nacos-net --format "{{range .Containers}}{{.Name}} {{end}}"
```

如果 MySQL 不在网络中：

```powershell
docker network connect nacos-net mysql-shared
```

如果 Nacos 启动时用了 `--network bridge`，删除重建：

```powershell
docker rm -f nacos-standalone
# 重新执行第六步的 docker run 命令
```

### 问题 2：`Communications link failure`（无 UnknownHostException）

**原因**：网络通了，但 MySQL 拒绝连接。

**排查**：

```powershell
# MySQL 是否在运行
docker ps --filter "name=mysql-shared"

# 密码是否正确
docker exec -it mysql-shared mysql -uroot -p123456 -e "SELECT 1;"
```

### 问题 3：`Table 'nacos_config.config_info' doesn't exist`

**原因**：建表脚本未导入，或导入到了错误的库。

**修复**：重新执行第四步 4.3 导入脚本，并确认命令末尾是 `nacos_config`。

### 问题 4：`Access denied for user 'root'`

**原因**：`MYSQL_SERVICE_PASSWORD` 与 MySQL 实际密码不一致。

**修复**：确认 `docker run` 中 `-e MYSQL_ROOT_PASSWORD=123456` 与 `-e MYSQL_SERVICE_PASSWORD=123456` 一致。

### 问题 5：端口 3306 被占用

**原因**：本机 MySQL93 服务又启动了。

**修复**：

```powershell
net stop mysql93
```

或把 Docker MySQL 改为 `-p 3307:3306`（此时 Windows 侧连接用 3307，容器间仍用 3306，Nacos 配置不变）。

### 问题 6：页面 `ERR_CONNECTION_REFUSED`

**原因**：Nacos 容器已退出。

**排查**：

```powershell
docker ps -a --filter "name=nacos-standalone"
docker logs --tail 200 nacos-standalone
```

按日志报错对号入座，优先检查上面 1~5 项。

---

## 九、日常运维命令速查

| 操作 | 命令 |
|---|---|
| 查看所有容器 | `docker ps -a` |
| 启动 MySQL | `docker start mysql-shared` |
| 启动 Nacos | `docker start nacos-standalone` |
| 停止 Nacos | `docker stop nacos-standalone` |
| 重启 Nacos | `docker restart nacos-standalone` |
| 查看 Nacos 日志 | `docker logs -f nacos-standalone` |
| 进入 MySQL | `docker exec -it mysql-shared mysql -uroot -p123456` |
| 查看网络成员 | `docker network inspect nacos-net` |
| 备份 MySQL 数据 | `docker exec mysql-shared mysqldump -uroot -p123456 nacos_config > E:\nacos_backup.sql` |

---

## 十、部署流程总览

```
┌─────────────────────────────────────────────────┐
│ 1. net stop mysql93          （释放 3306）       │
│ 2. docker network create nacos-net               │
│ 3. docker run mysql-shared   （加入 nacos-net）  │
│ 4. 创建 nacos_config 库 + 导入 mysql-schema.sql  │
│ 5. （可选）从本机 MySQL 迁移旧数据               │
│ 6. docker run nacos-standalone（加入 nacos-net） │
│ 7. 日志出现 "use external storage" → 成功        │
│ 8. 访问 http://localhost:8848/nacos              │
└─────────────────────────────────────────────────┘
```

---

## 📌 关键注意事项

1. **网络一致性**：MySQL 和 Nacos **必须**在同一个自定义网络 `nacos-net`，否则容器名无法解析。
2. **端口冲突**：本机 MySQL93 与 Docker MySQL 不能同时占用 3306，切换前先 `net stop` / `net start`。
3. **版本匹配**：SQL 脚本、镜像 tag、`docker run` 中的版本三者必须一致（本流程统一用 `2.4.3`）。
4. **数据持久化**：`mysql-data` 卷保存数据，删除容器不影响数据；只有 `docker volume rm mysql-data` 才会清空。
5. **密码一致性**：`MYSQL_ROOT_PASSWORD`、`MYSQL_SERVICE_PASSWORD`、`--health-cmd` 中的密码三处必须一致。
6. **PowerShell 语法**：`docker run` 用**单行**命令，不要用 `\` 换行；PowerShell 续行符是反引号 `` ` ``，且反引号后不能有空格。