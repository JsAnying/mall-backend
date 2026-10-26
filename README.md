# AiSpingboot：注册接口分层工程

当前技术栈：Java 17、Spring Boot 3.5.16、MyBatis-Plus 3.5.17、MySQL、Bean Validation、Lombok。包名为 `org.example.aispingboot`，启动类为 `AiSpingbootApplication`，按用户指定保留该拼写。

本轮只实现注册。登录示例中的用户名/邮箱查询、密码匹配和登录 DTO 不在本轮范围内；没有新增登录接口、邮箱字段或 JWT。独立 BCrypt 工具仅负责密码哈希，不启用 Spring Security Web 鉴权。

## 目录结构

```text
src/main/java/org/example/aispingboot/
├── AiSpingbootApplication.java
├── common/
│   ├── GlobalExceptionHandler.java
│   ├── Result.java
│   └── ResultCode.java
├── config/
│   ├── JsonConfig.java
│   └── PasswordConfig.java
├── controller/UserController.java
├── dto/
│   ├── command/UserRegisterCommandDTO.java
│   └── response/UserRegisterResponseDTO.java
├── entity/User.java
├── exception/BusinessException.java
├── mapper/UserMapper.java
└── service/
    ├── UserService.java
    └── impl/UserServiceImpl.java
```

调用方向为 Controller → Service → Mapper。Controller 只依赖 UserService；Mapper 只继承 BaseMapper，不编写业务判断。依赖字段统一通过 `jakarta.annotation.Resource` 注入，入参校验使用 Boot 3 对应的 `jakarta.validation`，不混用 `javax.validation`。

## 先验证工程

本机已安装的 Java 17 路径如下。其他机器请替换为实际 JDK 17 路径，此设置只影响当前终端：

```powershell
cd E:\shiguang-server\market
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17.0.14.7-hotspot'
mvn.cmd -version
mvn.cmd clean test
```

测试使用 H2 内存库，不需要 MySQL 凭据。测试清理前会检查数据库产品名必须为 H2。正常运行不激活 test profile，测试表结构不会用于开发 MySQL。

## MySQL 初始化与旧账号兼容

当前实体明确映射到带引号的 `user` 表。旧工程使用 `users`；切换实体名不会自动迁移旧数据，也不会自动更新商品、订单等旧表外键。

1. 确认开发库 `shiguang_market_dev` 已存在。如果使用其他库，同步调整 SQL 的 USE 和环境变量 DB_URL。
2. 在数据库客户端手动执行 [01-user-schema.sql](sql/01-user-schema.sql)，仅创建新 user 表。若已有同名表，先核对结构；IF NOT EXISTS 不是自动升级工具。
3. 若要沿用旧注册账号，确认新 user 表为空，再手动执行 [02-copy-legacy-users.sql](sql/02-copy-legacy-users.sql)。ID 和 BCrypt 哈希会原样复制；旧 users 表保持不变。该可选脚本不要重复执行。
4. 旧市场 15 表脚本仍作为后续业务参考保存。当前只重构注册，将来对接商品、订单时需要另行统一用户外键，不能假定它们已迁移。

本轮不会自动执行这两个脚本，也没有连接真实 MySQL。

## 启动应用

在实际启动进程的环境变量或 IDE Run Configuration 中配置：

| 变量 | 用途 |
| --- | --- |
| DB_USERNAME | 数据库账号，默认 shiguang_dev |
| DB_PASSWORD | 数据库密码，必须配置，不写入源码 |
| DB_URL | 可选，默认连接 localhost:3306/shiguang_market_dev |

选择 JDK 17 后运行 `org.example.aispingboot.AiSpingbootApplication`，或在上述终端执行：

```powershell
mvn.cmd spring-boot:run
```

也可以先执行 `mvn.cmd -DskipTests package`，再通过 JDK 17 的 `java -jar target/aispingboot-0.0.1-SNAPSHOT.jar` 启动。两种方式都需要先完成 MySQL 建表并配置数据库环境变量。

## 注册接口

接口保持 `POST http://localhost:8080/api/auth/register`，前端现有调用路径不变。

```json
{"username":"xiaolin","password":"Example123!"}
```

- username：去除首尾空白后，3～32 位字母、数字或下划线。
- password：必须非空白、6～64 位、UTF-8 编码不超过 72 字节；不自动 trim。
- 用户名不区分大小写唯一，由数据库排序规则和唯一索引保证。
- 输入不接受用户 ID，响应不返回实体或密码字段。

成功返回 HTTP 200：

```json
{"code":200,"msg":"操作成功","data":{"id":"10001","username":"xiaolin"}}
```

重复用户名返回 HTTP 409：

```json
{"code":409,"msg":"用户名已存在，请前往登录或更换用户名","data":null}
```

非法输入返回 400；内部错误返回 500；数据库资源不可用返回 503。所有返回体只包含 code、msg、data。业务层不返回 Result，也不吞异常，错误由 common.GlobalExceptionHandler 统一包装。

## 关键实现

注册查重使用 `new LambdaQueryWrapper<User>().eq(User::getUsername, command.getUsername())`。MyBatis-Plus 自动生成查询和插入 SQL，不再使用旧 Mapper XML。实体通过 `@TableId(type = IdType.AUTO)` 接收数据库生成的 ID，再转换成响应 DTO 的字符串 ID。

查重和插入之间仍可能发生竞争，因此 Service 同时捕获用户名唯一约束冲突。密码以 BCrypt 哈希保存，注册后由前端转到登录页，不在注册接口自动登录。

## 完整源码与维护记录

- [所有文件的完整代码](docs/refactor-complete-source.md)：根据当前实际文件生成，包括生产代码、配置、SQL 和测试，不使用省略号。
- [开发建议与修错记录](docs/development-lessons.md)：持续追加本轮及之后的修复经验。

版本集成参考 [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html) 和 [MyBatis-Plus 官方安装说明](https://baomidou.com/getting-started/install/)。Plus 使用 Boot 3 专用 starter，不同时引入原生 MyBatis starter。
