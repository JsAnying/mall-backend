# 注册接口重构：每个文件的完整代码

生成日期：2026-10-25。技术栈：Java 17、Spring Boot 3.5.16、MyBatis-Plus 3.5.17。只实现注册，主包 org.example.aispingboot。

本文件根据项目实际源码生成，包含 pom、全部生产与测试代码、配置和 SQL，未省略任何文件内容。运行方式见 ../README.md；数据库脚本需手动执行。

文件总数：25

## pom.xml

```xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.16</version>
        <relativePath/>
    </parent>
    <groupId>org.example</groupId>
    <artifactId>aispingboot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>aispingboot</name>
    <description>用户注册分层示例</description>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.17</mybatis-plus.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <!-- 只引入 Boot 3 对应的 Plus starter，不叠加原生 MyBatis starter。 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <!-- 仅用于 BCrypt 密码哈希，不启用 Web 鉴权。 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <release>17</release>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```

## sql/01-user-schema.sql

```sql

-- 当前注册工程的 MySQL 8+ 建表脚本，仅新建 user 表，不修改旧 users 表。
USE shiguang_market_dev;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_file_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT ck_user_username CHECK (REGEXP_LIKE(username, '^[A-Za-z0-9_]{3,32}$')),
    CONSTRAINT ck_user_nickname CHECK (CHAR_LENGTH(TRIM(nickname)) BETWEEN 1 AND 32)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户注册账号';

```

## sql/02-copy-legacy-users.sql

```sql

-- 可选：在确认目标 user 表为空、列结构正确后，手动复制旧账号。
-- 先执行 01-user-schema.sql。本脚本只读旧 users 表，不删除或更名任何表。
-- 不使用 INSERT IGNORE 或 REPLACE；存在 ID/用户名冲突时应报错并停止处理。
USE shiguang_market_dev;

INSERT INTO `user` (id, username, password_hash, nickname, avatar_file_id, created_at, updated_at)
SELECT id, username, password_hash, nickname, avatar_file_id, created_at, updated_at
FROM users;

```

## src/main/java/org/example/aispingboot/AiSpingbootApplication.java

```java

package org.example.aispingboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 应用启动入口，组件扫描覆盖当前根包。 */
@SpringBootApplication
public class AiSpingbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpingbootApplication.class, args);
    }
}

```

## src/main/java/org/example/aispingboot/common/GlobalExceptionHandler.java

```java

package org.example.aispingboot.common;

import org.example.aispingboot.exception.BusinessException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 将业务异常和框架异常统一转换为 code、msg、data 三字段响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** 处理校验失败、用户名重复等可预期的业务错误。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException exception) {
        ResultCode resultCode = exception.getResultCode();
        return ResponseEntity.status(resultCode.getCode())
                .body(Result.error(resultCode, exception.getMessage()));
    }

    /** 处理 Controller 中 @Valid 触发的请求参数校验错误。 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String msg = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(ResultCode.BAD_REQUEST.getMsg());
        return buildResponse(ResultCode.BAD_REQUEST.getCode(), msg, headers);
    }

    /** 拒绝空请求体、畸形 JSON 和字段类型错误。 */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return buildResponse(ResultCode.INVALID_JSON.getCode(), ResultCode.INVALID_JSON.getMsg(), headers);
    }

    /** 保留 Spring MVC 的真实 HTTP 状态，并替换其默认返回体。 */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String msg = switch (status.value()) {
            case 400 -> ResultCode.BAD_REQUEST.getMsg();
            case 401 -> ResultCode.UNAUTHORIZED.getMsg();
            case 403 -> ResultCode.FORBIDDEN.getMsg();
            case 404 -> ResultCode.NOT_FOUND.getMsg();
            case 405 -> ResultCode.METHOD_NOT_ALLOWED.getMsg();
            case 406 -> ResultCode.NOT_ACCEPTABLE.getMsg();
            case 409 -> ResultCode.CONFLICT.getMsg();
            case 413 -> ResultCode.FILE_TOO_LARGE.getMsg();
            case 415 -> ResultCode.UNSUPPORTED_MEDIA_TYPE.getMsg();
            case 429 -> ResultCode.RATE_LIMITED.getMsg();
            case 503 -> ResultCode.SERVICE_UNAVAILABLE.getMsg();
            default -> ResultCode.INTERNAL_ERROR.getMsg();
        };
        return buildResponse(status.value(), msg, headers);
    }

    /** 数据库资源不可用或暂时性故障返回 503，不暴露 SQL 或连接信息。 */
    @ExceptionHandler({TransientDataAccessException.class, NonTransientDataAccessResourceException.class})
    public ResponseEntity<Result<Void>> handleDatabase(DataAccessException exception) {
        log.error("数据库访问失败，异常类型：{}", exception.getClass().getName());
        return ResponseEntity.status(ResultCode.SERVICE_UNAVAILABLE.getCode())
                .body(Result.error(ResultCode.SERVICE_UNAVAILABLE));
    }

    /** 未预期的内部错误返回 500，避免将异常详情发送给客户端。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception exception) {
        log.error("请求处理失败，异常类型：{}", exception.getClass().getName());
        return ResponseEntity.status(ResultCode.INTERNAL_ERROR.getCode())
                .body(Result.error(ResultCode.INTERNAL_ERROR));
    }

    private ResponseEntity<Object> buildResponse(int code, String msg, HttpHeaders headers) {
        return new ResponseEntity<>(Result.error(code, msg), headers, HttpStatusCode.valueOf(code));
    }
}

```

## src/main/java/org/example/aispingboot/common/Result.java

```java

package org.example.aispingboot.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 统一接口返回体，只包含响应码、提示消息和业务数据。
 *
 * @param <T> 业务数据类型
 */
@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    /** 响应码，与 HTTP 状态码保持一致。 */
    private final int code;

    /** 可直接展示给用户的提示消息。 */
    private final String msg;

    /** 业务数据，无返回内容或操作失败时为 null。 */
    private final T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 创建携带业务数据的成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 创建不携带业务数据的成功响应。 */
    public static Result<Void> success() {
        return success(null);
    }

    /** 使用枚举中的默认提示创建失败响应。 */
    public static Result<Void> error(ResultCode resultCode) {
        return error(resultCode, resultCode.getMsg());
    }

    /** 使用枚举响应码和具体提示创建失败响应。 */
    public static Result<Void> error(ResultCode resultCode, String msg) {
        return error(resultCode.getCode(), msg);
    }

    /** 适配 Spring MVC 等框架产生的 HTTP 错误状态。 */
    public static Result<Void> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}

```

## src/main/java/org/example/aispingboot/common/ResultCode.java

```java

package org.example.aispingboot.common;

/**
 * 项目统一响应码及默认中文提示。
 * 同一 HTTP 状态可对应不同业务提示，响应体不额外增加业务标识字段。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数有误"),
    INVALID_JSON(400, "请求体必须是有效的 JSON，字段类型须正确"),
    UNAUTHORIZED(401, "请先登录"),
    TOKEN_EXPIRED(401, "登录已过期，请重新登录"),
    INVALID_CREDENTIALS(401, "用户名或密码错误"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    GOODS_NOT_FOUND(404, "商品不存在或已删除"),
    ORDER_NOT_FOUND(404, "订单不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    NOT_ACCEPTABLE(406, "请求的响应格式不支持"),
    CONFLICT(409, "资源状态冲突，请刷新后重试"),
    USER_NOT_FOUND(404, "用户不存在"),
    PASSWORD_ERROR(401, "密码错误"),
    USERNAME_EXISTS(409, "用户名已存在，请前往登录或更换用户名"),
    GOODS_UNAVAILABLE(409, "商品已被预订，请选择其他商品"),
    INVALID_STATE(409, "当前状态不允许执行此操作"),
    IDEMPOTENCY_CONFLICT(409, "重复请求的内容不一致"),
    FILE_TOO_LARGE(413, "上传文件过大"),
    UNSUPPORTED_MEDIA_TYPE(415, "请求格式不支持，请使用 application/json"),
    UNSUPPORTED_FILE_TYPE(415, "上传文件类型不支持"),
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "服务内部错误，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用，请稍后重试");

    /** 与 HTTP 状态对应的响应码。 */
    private final int code;

    /** 可直接展示给用户的默认提示。 */
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 获取响应码。 */
    public int getCode() {
        return code;
    }

    /** 获取默认中文提示。 */
    public String getMsg() {
        return msg;
    }
}

```

## src/main/java/org/example/aispingboot/config/JsonConfig.java

```java

package org.example.aispingboot.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 严格校验 JSON 字符串类型，禁止将数字或布尔值静默转成注册凭据。 */
@Configuration
public class JsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictStringFields() {
        return builder -> builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}

```

## src/main/java/org/example/aispingboot/config/PasswordConfig.java

```java

package org.example.aispingboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 独立的密码哈希工具配置，不启用 Spring Security 的 Web 鉴权功能。 */
@Configuration
public class PasswordConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

```

## src/main/java/org/example/aispingboot/controller/UserController.java

```java

package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.dto.response.UserRegisterResponseDTO;
import org.example.aispingboot.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户接口，只负责接收请求、调用业务层及包装返回结果。 */
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 注册新用户，不直接返回数据库实体或签发登录令牌。
     *
     * @param command 用户名和密码
     * @return 新用户的公开资料
     */
    @PostMapping("/register")
    public Result<UserRegisterResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO command) {
        return Result.success(userService.register(command));
    }
}

```

## src/main/java/org/example/aispingboot/dto/command/UserRegisterCommandDTO.java

```java

package org.example.aispingboot.dto.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 注册请求参数，只接收用户名和密码。
 * 用户名在创建对象时标准化；密码保持原样，且不会出现在日志或 JSON 输出中。
 */
@Data
@NoArgsConstructor
public class UserRegisterCommandDTO {
    /** 3～32 位字母、数字或下划线，首尾空白会被移除。 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名必须为 3～32 位字母、数字或下划线")
    private String username;

    /** 密码长度为 6～64 位，UTF-8 最多 72 字节。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 位之间")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 显式指定 JSON 构造入口，保证标准化发生在 Bean Validation 校验之前。 */
    @JsonCreator
    public UserRegisterCommandDTO(@JsonProperty("username") String username,
                           @JsonProperty("password") String password) {
        setUsername(username);
        this.password = password;
    }

    // JSON 反序列化时先标准化用户名，再校验；密码不做 trim。
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    /** BCrypt 输入存在字节上限，字符数校验不能代替此检查。 */
    @JsonIgnore
    @AssertTrue(message = "密码的 UTF-8 编码不能超过 72 字节")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}

```

## src/main/java/org/example/aispingboot/dto/response/UserRegisterResponseDTO.java

```java

package org.example.aispingboot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册成功后返回的公开资料，不包含密码及密码哈希。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterResponseDTO {

    /** 用户 ID 使用字符串，避免前端数字精度丢失。 */
    private String id;

    /** 去除首尾空白后的用户名。 */
    private String username;
}

```

## src/main/java/org/example/aispingboot/entity/User.java

```java

package org.example.aispingboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 用户数据库实体，对应 user 表。
 */
@TableName("`user`")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // 数据库自增主键，对应 BIGINT。
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，由数据库唯一约束保证唯一性。 */
    private String username;

    // 只保存 BCrypt 哈希，不接收或返回 JSON，也不参与 toString 输出。
    @JsonIgnore
    @ToString.Exclude
    private String passwordHash;

    /** 对外展示的昵称，注册时默认使用用户名。 */
    private String nickname;

    // 头像文件 ID，可为 null；不是头像 URL。
    private String avatarFileId;

    // 对应数据库 DATETIME(3)，按项目约定以 UTC 存取。
    private LocalDateTime createdAt;

    /** 最近更新时间，按项目约定以 UTC 存取。 */
    private LocalDateTime updatedAt;
}

```

## src/main/java/org/example/aispingboot/exception/BusinessException.java

```java

package org.example.aispingboot.exception;

import org.example.aispingboot.common.ResultCode;
import lombok.Getter;

/**
 * 可向用户展示的业务异常，由全局异常处理器转换为统一返回体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误对应的响应码与默认提示。 */
    private final ResultCode resultCode;

    /** 使用枚举的默认提示。 */
    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getMsg());
    }

    /** 在保留统一响应码的同时，提供具体业务提示。 */
    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.resultCode = resultCode;
    }
}

```

## src/main/java/org/example/aispingboot/mapper/UserMapper.java

```java

package org.example.aispingboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispingboot.entity.User;

/** 仅提供数据库操作，通用 CRUD 由 MyBatis-Plus 实现。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

```

## src/main/java/org/example/aispingboot/service/impl/UserServiceImpl.java

```java

package org.example.aispingboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.dto.response.UserRegisterResponseDTO;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.UserMapper;
import org.example.aispingboot.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户注册业务，依次完成校验、查重、密码哈希和持久化。 */
@Service
public class UserServiceImpl implements UserService {

    private static final String USERNAME_UNIQUE_CONSTRAINT = "uk_user_username";

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private Validator validator;

    /**
     * Controller 和 Service 共用 Command DTO 的约束，确保直接调用也受到校验。
     * Lambda 条件使用实体方法引用，不在业务代码中拼写数据库列名。
     */
    @Override
    @Transactional
    public UserRegisterResponseDTO register(UserRegisterCommandDTO command) {
        validate(command);
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, command.getUsername());
        if (userMapper.selectOne(query) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = createUser(command);
        int rows;
        try {
            // 必须插入同一个实体，IdType.AUTO 会把生成的主键回填到实体中。
            rows = userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            // 先查询不能防止并发竞争，最终依赖数据库唯一约束。
            if (isUsernameConflict(exception)) {
                throw new BusinessException(ResultCode.USERNAME_EXISTS);
            }
            throw exception;
        }
        if (rows != 1 || user.getId() == null) {
            throw new IllegalStateException("用户插入或主键回填失败");
        }
        return new UserRegisterResponseDTO(user.getId().toString(), user.getUsername());
    }

    private void validate(UserRegisterCommandDTO command) {
        if (command == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "注册信息不能为空");
        }
        validator.validate(command).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .findFirst()
                .ifPresent(msg -> {
                    throw new BusinessException(ResultCode.BAD_REQUEST, msg);
                });
    }

    /** 密码保持原样，只存 BCrypt 哈希；时间以 UTC 写入。 */
    private User createUser(UserRegisterCommandDTO command) {
        User user = new User();
        user.setUsername(command.getUsername());
        user.setPasswordHash(passwordEncoder.encode(command.getPassword()));
        user.setNickname(command.getUsername());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    /** 不把其他唯一键或数据库故障误报成用户名重复。 */
    private boolean isUsernameConflict(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String msg = cause.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains(USERNAME_UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }
}

```

## src/main/java/org/example/aispingboot/service/UserService.java

```java

package org.example.aispingboot.service;

import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.dto.response.UserRegisterResponseDTO;

/** 用户业务接口，当前只实现注册。 */
public interface UserService {

    /**
     * 注册用户，失败时抛出业务异常。
     *
     * @param command 注册参数
     * @return 注册成功的公开资料
     */
    UserRegisterResponseDTO register(UserRegisterCommandDTO command);
}

```

## src/main/resources/application.yaml

```yaml

server:
  port: 8080

spring:
  application:
    name: aispingboot
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/shiguang_market_dev?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC}
    username: ${DB_USERNAME:shiguang_dev}
    password: ${DB_PASSWORD}
  sql:
    init:
      # MySQL 建表脚本需手动执行，应用不会自动更改已有数据库。
      mode: never

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 30
  global-config:
    banner: false

```

## src/test/java/org/example/aispingboot/AiSpingbootApplicationTests.java

```java

package org.example.aispingboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiSpingbootApplicationTests {

    @Test
    void contextLoads() {
    }

}

```

## src/test/java/org/example/aispingboot/LayeringTests.java

```java

package org.example.aispingboot;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.annotation.Resource;
import java.lang.reflect.Modifier;
import org.example.aispingboot.controller.UserController;
import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.dto.response.UserRegisterResponseDTO;
import org.example.aispingboot.mapper.UserMapper;
import org.example.aispingboot.service.UserService;
import org.example.aispingboot.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

/** 防止重构后 Controller 越层访问 Mapper，或直接将实体暴露给前端。 */
class LayeringTests {

    @Test
    void controllerOnlyDependsOnServiceAndReturnsAResponseDto() throws Exception {
        assertThat(UserController.class.getDeclaredFields()).hasSize(1);
        assertThat(UserController.class.getDeclaredFields()[0].getType()).isEqualTo(UserService.class);
        assertThat(UserController.class.getMethod("register", UserRegisterCommandDTO.class)
                .getGenericReturnType().getTypeName()).contains(UserRegisterResponseDTO.class.getName());
    }

    @Test
    void mapperOnlyInheritsDatabaseOperations() {
        assertThat(BaseMapper.class.isAssignableFrom(UserMapper.class)).isTrue();
        assertThat(UserMapper.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    void productionDependenciesUseResourceFieldInjection() {
        for (Class<?> type : new Class<?>[]{UserController.class, UserServiceImpl.class}) {
            for (var field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    assertThat(field.isAnnotationPresent(Resource.class)).as(field.toString()).isTrue();
                }
            }
        }
    }
}

```

## src/test/java/org/example/aispingboot/RegistrationFailureResponseTests.java

```java

package org.example.aispingboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.aispingboot.controller.UserController;
import org.example.aispingboot.common.GlobalExceptionHandler;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.service.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import jakarta.annotation.Resource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 验证内部故障的 HTTP 状态和三字段返回体，避免泄露敏感信息。 */
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class RegistrationFailureResponseTests {

    @Resource
    private MockMvc mvc;

    @Resource
    private ObjectMapper json;

    @MockitoBean
    private UserService service;

    @Test
    void databaseOutageReturns503WithoutLeakingInternalDetails() throws Exception {
        when(service.register(any())).thenThrow(new DataAccessResourceFailureException("private database details"));
        assertSafeFailure(ResultCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void programmingErrorReturns500WithoutLeakingInternalDetails() throws Exception {
        when(service.register(any())).thenThrow(new IllegalStateException("private internal details"));
        assertSafeFailure(ResultCode.INTERNAL_ERROR);
    }

    @Test
    void sqlProgrammingErrorIsNotReportedAsDatabaseOutage() throws Exception {
        when(service.register(any())).thenThrow(new BadSqlGrammarException(
                "private task", "private SQL", new java.sql.SQLException("private database details")));
        assertSafeFailure(ResultCode.INTERNAL_ERROR);
    }

    private void assertSafeFailure(ResultCode resultCode) throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test_user\",\"password\":\"example123\"}"))
                .andExpect(status().is(resultCode.getCode()))
                .andExpect(jsonPath("$.code").value(resultCode.getCode()))
                .andExpect(jsonPath("$.msg").value(resultCode.getMsg()))
                .andReturn();
        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(response).contains("\"data\":null").doesNotContain("private", "example123", "SQLException");
        var body = json.readTree(response);
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.has("code") && body.has("msg") && body.has("data")).isTrue();
    }
}

```

## src/test/java/org/example/aispingboot/RegistrationIntegrationTests.java

```java

package org.example.aispingboot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.example.aispingboot.mapper.UserMapper;
import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.common.ResultCode;
import jakarta.validation.Validator;
import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.service.impl.UserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jakarta.annotation.Resource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 验证注册接口、真实 Mapper 映射及三字段返回契约。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTests {
    @Resource MockMvc mvc;
    @Resource ObjectMapper json;
    @Resource UserMapper mapper;
    @Resource PasswordEncoder encoder;
    @Resource JdbcTemplate jdbc;
    @Resource Validator validator;

    @BeforeEach
    void resetOnlyTheEmbeddedTestDatabase() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        jdbc.update("DELETE FROM `user`");
    }

    @Test
    void registerPersistsHashAndReturnsTheActualGeneratedId() throws Exception {
        String password = "  example123  ";
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        MvcResult result = register("  Alice_01  ", password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.username").value("Alice_01"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();
        assertResponseFields(result);
        User saved = findUser("alice_01");
        assertThat(saved).isNotNull();
        assertThat(saved.getNickname()).isEqualTo("Alice_01");
        assertThat(saved.getPasswordHash()).isNotEqualTo(password).startsWith("$2");
        assertThat(encoder.matches(password, saved.getPasswordHash())).isTrue();
        assertThat(encoder.matches(password.trim(), saved.getPasswordHash())).isFalse();
        assertThat(saved.getCreatedAt()).isAfter(before).isBefore(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(json.readTree(result.getResponse().getContentAsString()).at("/data/id").asText())
                .isEqualTo(saved.getId().toString());
        MvcResult second = register("second_user", "example123").andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(second.getResponse().getContentAsString()).at("/data/id").asText())
                .isNotEqualTo(saved.getId().toString());
    }

    @Test
    void duplicateUsernameIncludingCaseReturns409AndDoesNotCreateAnotherUser() throws Exception {
        register("Alice_01", "example123").andExpect(status().isOk());
        MvcResult result = register("  ALICE_01 ", "different123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value(ResultCode.USERNAME_EXISTS.getMsg()))
                .andReturn();
        assertErrorEnvelope(result);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isEqualTo(1);
        assertThat(encoder.matches("example123", findUser("Alice_01").getPasswordHash())).isTrue();
    }

    @Test
    void simultaneousRegistrationsAreArbitratedByTheRealUniqueConstraint() throws Exception {
        CyclicBarrier bothQueriesCompleted = new CyclicBarrier(2);
        UserMapper racingMapper = mock(UserMapper.class, delegatesTo(mapper));
        doAnswer(invocation -> {
            Wrapper<User> query = invocation.getArgument(0);
            User saved = mapper.selectOne(query);
            bothQueriesCompleted.await(5, TimeUnit.SECONDS);
            return saved;
        }).when(racingMapper).selectOne(any(Wrapper.class));
        UserServiceImpl racingService = new UserServiceImpl();
        ReflectionTestUtils.setField(racingService, "userMapper", racingMapper);
        ReflectionTestUtils.setField(racingService, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(racingService, "validator", validator);
        java.util.concurrent.Callable<Integer> register = () -> {
            try {
                racingService.register(new UserRegisterCommandDTO("racing_user", "example123"));
                return 200;
            } catch (BusinessException exception) {
                assertThat(exception.getResultCode()).isEqualTo(ResultCode.USERNAME_EXISTS);
                return exception.getResultCode().getCode();
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(register);
            var second = executor.submit(register);
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isEqualTo(1);
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of(null, "example123"), Arguments.of("", "example123"),
                Arguments.of("   ", "example123"), Arguments.of("ab", "example123"),
                Arguments.of("a".repeat(33), "example123"), Arguments.of("bad-name", "example123"),
                Arguments.of("中文用户名", "example123"), Arguments.of("user_01", null),
                Arguments.of("user_01", ""), Arguments.of("user_01", "      "), Arguments.of("user_01", "12345"),
                Arguments.of("user_01", "a".repeat(65)), Arguments.of("user_01", "密".repeat(25)));
    }

    @ParameterizedTest
    @MethodSource("invalidCredentials")
    void invalidFieldsReturn400AndNeverInsert(String username, String password) throws Exception {
        MvcResult result = register(username, password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andReturn();
        assertErrorEnvelope(result);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isZero();
    }

    static Stream<Arguments> validBoundaries() {
        return Stream.of(Arguments.of("abc", "123456"),
                Arguments.of("a".repeat(32), "p".repeat(64)),
                Arguments.of("multibyte_user", "密".repeat(24)));
    }

    @ParameterizedTest
    @MethodSource("validBoundaries")
    void validLengthAnd72ByteBoundariesAreAccepted(String username, String password) throws Exception {
        register(username, password).andExpect(status().isOk());
        assertThat(encoder.matches(password, findUser(username).getPasswordHash())).isTrue();
    }

    @Test
    void missingFieldsMalformedJsonAndWrongContentTypeHaveJsonErrors() throws Exception {
        for (String body : new String[]{"{}", "null", "", "{bad json",
                "{\"username\":[],\"password\":\"example123\"}",
                "{\"username\":123,\"password\":\"example123\"}",
                "{\"username\":\"test_user\",\"password\":123456}",
                "{\"username\":\"test_user\",\"password\":true}"}) {
            MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andReturn();
            assertErrorEnvelope(result);
        }
        mvc.perform(post("/api/auth/register").contentType(MediaType.TEXT_PLAIN).content("test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.msg").value(ResultCode.UNSUPPORTED_MEDIA_TYPE.getMsg()));
    }

    @Test
    void requestsReachMvcWithoutSecurityInterception() throws Exception {
        MvcResult result = mvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value(ResultCode.NOT_FOUND.getMsg())).andReturn();
        assertErrorEnvelope(result);
        mvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void passwordNeverAppearsInDtoLogsOrJson() throws Exception {
        UserRegisterCommandDTO request = new UserRegisterCommandDTO("test_user", "private-example123");
        assertThat(request.toString()).doesNotContain("private-example123");
        assertThat(json.writeValueAsString(request)).doesNotContain("private-example123", "passwordWithinByteLimit");
        User user = new User();
        user.setPasswordHash("private-hash");
        assertThat(user.toString()).doesNotContain("private-hash");
        assertThat(json.writeValueAsString(user)).doesNotContain("private-hash", "passwordHash");
    }

    private User findUser(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Test
    void loginRemainsOutsideTheCurrentScope() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user_01\",\"password\":\"example123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8).content(json.writeValueAsString(body)));
    }

    /** 精确检查返回体字段，禁止旧字段或新增字段混入接口。 */
    private void assertResponseFields(MvcResult result) throws Exception {
        var body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.isObject()).isTrue();
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.has("code") && body.has("msg") && body.has("data")).isTrue();
        assertThat(body.path("code").asInt()).isEqualTo(result.getResponse().getStatus());
        assertThat(body.path("msg").asText()).isNotBlank();
    }

    private void assertErrorEnvelope(MvcResult result) throws Exception {
        assertResponseFields(result);
        var body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("data").isNull()).isTrue();
    }
}

```

## src/test/java/org/example/aispingboot/UserServiceTests.java

```java

package org.example.aispingboot;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.example.aispingboot.exception.BusinessException;
import org.example.aispingboot.mapper.UserMapper;
import org.example.aispingboot.dto.command.UserRegisterCommandDTO;
import org.example.aispingboot.entity.User;
import org.example.aispingboot.service.impl.UserServiceImpl;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.example.aispingboot.common.ResultCode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 注册业务测试，重点验证直接调用、并发冲突和写入失败。 */
class UserServiceTests {
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void initializeValidation() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidation() {
        validatorFactory.close();
    }

    private UserMapper mapper;
    private PasswordEncoder encoder;
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        mapper = mock(UserMapper.class);
        encoder = mock(PasswordEncoder.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "validator", validatorFactory.getValidator());
    }

    @Test
    void raceAtTheUniqueConstraintBecomes409() {
        when(encoder.encode("example123")).thenReturn("hash");
        when(mapper.insert(any(User.class))).thenThrow(new DuplicateKeyException(
                "SQL insert failed", new java.sql.SQLIntegrityConstraintViolationException(
                        "Duplicate entry for key 'users.uk_user_username'")));
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("test_user", "example123")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getResultCode()).isEqualTo(ResultCode.USERNAME_EXISTS);
                });
    }

    @Test
    void existingUsernameStopsBeforeEncodingOrInserting() {
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(new User());
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("test_user", "example123")))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).insert(any(User.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void directCallsCannotBypassValidation() {
        for (UserRegisterCommandDTO request : new UserRegisterCommandDTO[]{null,
                new UserRegisterCommandDTO(null, "example123"), new UserRegisterCommandDTO("valid_user", null),
                new UserRegisterCommandDTO("valid_user", "密".repeat(25))}) {
            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOfSatisfying(BusinessException.class, exception ->
                            assertThat(exception.getResultCode()).isEqualTo(ResultCode.BAD_REQUEST));
        }
        verifyNoInteractions(mapper, encoder);
    }

    @Test
    void missingKeyOrUnexpectedRowCountCannotReturnSuccess() {
        when(encoder.encode("example123")).thenReturn("hash");
        when(mapper.insert(any(User.class))).thenReturn(1);
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isInstanceOf(IllegalStateException.class);
        when(mapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.<User>getArgument(0).setId(10001L);
            return 0;
        });
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unrelatedDuplicateKeyIsNotReportedAsUsernameConflict() {
        DuplicateKeyException failure = new DuplicateKeyException("Duplicate entry for key PRIMARY");
        when(mapper.insert(any(User.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isSameAs(failure);
    }
}

```

## src/test/resources/application-test.yaml

```yaml

spring:
  datasource:
    url: jdbc:h2:mem:registration-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:registration-schema.sql

```

## src/test/resources/registration-schema.sql

```sql

-- 仅用于 H2 内存测试，不会创建或清理开发 MySQL 数据。
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_file_id VARCHAR(64),
    created_at TIMESTAMP(3) NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username)
);

```
