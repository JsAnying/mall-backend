package com.zhf.mall.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import com.zhf.mall.common.Result;
import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserLoginResponseDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import com.zhf.mall.service.UserService;
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

    /**
     * 用户登录，不直接返回数据库实体或签发登录令牌。
     *
     * @return 新用户的公开资料
     */
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserRegisterCommandDTO command) {
        return Result.success(userService.login(command));
    }
}
