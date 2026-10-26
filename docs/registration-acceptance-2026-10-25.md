# 注册接口真实 MySQL 验收

验收日期：2026-10-25（北京时间，约 17:43）。结论：本轮 24 个真实 HTTP 场景和对应 MySQL 持久化检查全部通过，当前未复现历史数据库认证失败。

## 实际环境与连接

- 工程：E:/shiguang-server/market；实际根包 com.zhf.mall。
- 服务：http://localhost:8080；本轮监听进程 PID 2400，Microsoft JDK 17.0.14。
- 使用临时 JDK 附加诊断程序读取运行中 Spring 上下文的 DataSource，直接借用应用连接池连接；未用独立终端配置猜测实际凭据，也未更改启动配置。
- 元数据及持久化查询均使用只读连接，并在关闭前回滚；注册写入仅通过真实 HTTP 接口触发。
- 数据库：MySQL 9.6.0，shiguang_market_dev，端口 3306，认证账号 root@localhost。
- 会话与全局 time_zone 均为 SYSTEM；连接字符集 utf8mb4，连接排序规则 utf8mb4_0900_ai_ci。

## 实时表结构

user 和旧 users 表都存在。user 使用 InnoDB、utf8mb4_0900_ai_ci，包含当前实体的全部字段：

| 字段 | 实际类型/关键属性 |
| --- | --- |
| id | BIGINT，非空，自增主键 |
| username | VARCHAR(32)，非空，utf8mb4_0900_ai_ci |
| password_hash | VARCHAR(100)，非空 |
| nickname | VARCHAR(32)，非空 |
| avatar_file_id | VARCHAR(64)，可空，ascii_bin |
| created_at | DATETIME(3)，非空 |
| updated_at | DATETIME(3)，非空 |

已确认 PRIMARY、uk_user_username 唯一索引，以及 ck_user_nickname、ck_user_username CHECK 约束存在。没有执行建表、旧账号复制、更名或授权修改。

## HTTP 验证结果

除特别注明外，均为 POST /api/auth/register，Content-Type 为 application/json。

| 场景 | 数量 | 实际 HTTP 状态 |
| --- | --- | --- |
| 普通账号首次注册 | 1 | 200 |
| 用户名含首尾空格，密码为 72 个 UTF-8 字节且含首尾空格 | 1 | 200 |
| 相同用户名、大小写变体、首尾空格变体重复注册 | 3 | 409 |
| 空对象、缺密码、缺用户名、用户名 null、密码 null | 5 | 400 |
| 用户名 2 位、33 位、含非法字符 | 3 | 400 |
| 密码 5 位、65 位、全空格、UTF-8 为 75 字节 | 4 | 400 |
| 数字用户名、布尔密码 | 2 | 400 |
| 畸形 JSON、空请求体 | 2 | 400 |
| text/plain 请求 | 1 | 415 |
| GET /api/auth/register | 1 | 405 |
| POST /register | 1 | 404 |
| 合计 | 24 | 全部符合预期 |

每个场景都检查 Content-Type 为 JSON、返回体键严格等于 code/msg/data、code 等于 HTTP 状态且 msg 为字符串。失败时 data 为 null；成功 data 严格仅含 id、username，id 是数据库生成的正整数字符串。含空格用户名在响应中已标准化。

## MySQL 持久化复核

| 专用验收账号 | 实际 ID | 结果 |
| --- | --- | --- |
| acc_261025_174335_1ba3_a | 2 | 通过 |
| acc_261025_174335_1ba3_b | 3 | 通过 |

已逐项确认：

- 两个响应 ID 分别与实际数据库行一致，且互不相同。
- 实际用户名已去除首尾空格，默认昵称与用户名相同，avatar_file_id 为 null。
- password_hash 符合 BCrypt 格式，使用应用实际 PasswordEncoder.matches 验证原密码成功。
- 重复注册提交的另一密码不能匹配第一个账号，原密码继续匹配，证明重复请求未覆盖原密码。
- 第二个账号的密码恰为 72 个 UTF-8 字节；原值匹配，trim 后不匹配，证明密码首尾空格被保留。
- created_at 等于 updated_at；将 DATETIME 按 UTC 解释后，落在本次请求时间窗口内（允许 2 秒边界容差）。
- 本次随机验收前缀开始时 0 行，完成后恰为 2 行，失败请求没有增加该前缀记录。

两个专用验收账号保留在开发库中，未删除现有数据。测试密码由临时诊断程序在内存生成/使用；报告不保存密码或哈希。未写入旧 users 表。

## 修改范围与验证边界

本轮未修改业务源码、数据源配置、账号凭据、数据库授权或表结构。只更新开发建议和交接状态，并新增本报告；保留所有既有未提交改动。

本报告证明运行中 JDK 17 应用的真实 HTTP + MySQL 行为，不是 H2 测试结果。本轮未重新运行 Maven/H2 测试，未做浏览器或 Apifox 界面联调，未测试并发竞争、数据库故障或连接中断；没有实现或验收登录、JWT 及其他业务接口。历史 1045 的当前连接已恢复，不能据此确认历史错误具体是密码还是主机授权导致。
