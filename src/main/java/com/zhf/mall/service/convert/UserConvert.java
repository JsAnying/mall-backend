package com.zhf.mall.service.convert;

import com.zhf.mall.dto.response.UserLoginResponseDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import com.zhf.mall.entity.User;

// 用户转换类
public class UserConvert {

    // 用户注册响应DTO
    public static UserLoginResponseDTO toUserLoginResponseDTO(User user, String token) {
        return UserLoginResponseDTO.builder()
                .token(token)
                .userInfo(new UserRegisterResponseDTO(user.getId().toString(), user.getUsername()))
                .build();
    }
}
