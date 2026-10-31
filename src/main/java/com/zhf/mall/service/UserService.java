package com.zhf.mall.service;

import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserLoginResponseDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import jakarta.validation.Valid;

/** 用户业务接口，当前只实现注册。 */
public interface UserService {

    /**
     * 注册用户，失败时抛出业务异常。
     *
     * @param command 注册参数
     * @return 注册成功的公开资料
     */
    UserRegisterResponseDTO register(UserRegisterCommandDTO command);

    /**
     * 用户登录，不直接返回数据库实体或签发登录令牌。
     *
     * @param command 登录参数
     * @return 登录用户的公开资料
     */
    UserLoginResponseDTO login(@Valid UserRegisterCommandDTO command);

    /**
     * 用户登出。
     *
     * @param tokenId 已验证的令牌编号
     * @param expiresAt 令牌过期时间
     */
    void logout(String tokenId, java.util.Date expiresAt);
}
