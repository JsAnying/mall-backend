package com.zhf.mall.service.convert;

import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserLoginResponseDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import com.zhf.mall.entity.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

// 用户转换类
public class UserConvert {

    /**
     * 由注册参数构建待插入的用户实体。
     *
     * @param command         注册参数
     * @param encodedPassword 已哈希的密码
     * @return 待插入的用户实体
     */
    public static User toUserEntity(UserRegisterCommandDTO command, String encodedPassword) {
        User user = new User();
        user.setUsername(command.getUsername());
        user.setPasswordHash(encodedPassword);
        user.setNickname(command.getUsername()); // 昵称默认与用户名相同
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    // 用户注册响应DTO
    public static UserLoginResponseDTO toUserLoginResponseDTO(User user, String token) {
        return UserLoginResponseDTO.builder()
                .token(token)
                .userInfo(new UserRegisterResponseDTO(user.getId().toString(), user.getUsername()))
                .build();
    }
}
