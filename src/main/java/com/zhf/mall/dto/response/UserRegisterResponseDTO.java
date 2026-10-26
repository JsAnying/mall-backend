package com.zhf.mall.dto.response;

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
