package com.zhf.mall.entity;

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
