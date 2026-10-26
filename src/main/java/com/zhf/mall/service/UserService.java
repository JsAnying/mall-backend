package com.zhf.mall.service;

import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;

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
