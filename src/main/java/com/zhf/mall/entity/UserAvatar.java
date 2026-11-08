package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户头像文件，对应 user_avatar 表。
 */
@TableName("user_avatar")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatar {

    /** 业务生成的 UUID，不是数据库自增主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 头像所属用户 ID，对应 user.id。 */
    private Long userId;

    /** 文件 MIME 类型，仅允许 image/png、image/jpeg。 */
    private String contentType;

    /** 头像二进制内容。 */
    private byte[] data;
}
