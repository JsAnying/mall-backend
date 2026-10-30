package com.zhf.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhf.mall.dto.response.UserLoginResponseDTO;
import com.zhf.mall.service.convert.UserConvert;
import com.zhf.mall.util.JwtUtil;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import com.zhf.mall.common.ResultCode;
import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import com.zhf.mall.entity.User;
import com.zhf.mall.exception.BusinessException;
import com.zhf.mall.mapper.UserMapper;
import com.zhf.mall.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** 用户注册业务，依次完成校验、查重、密码哈希和持久化。 */
@Service
public class UserServiceImpl implements UserService {

    private static final String USERNAME_UNIQUE_CONSTRAINT = "uk_user_username";

    @Resource
    private UserMapper userMapper; // 用户Mapper


    @Resource
    private Validator validator; // 校验器

    // 密码加密器
    public final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Controller 和 Service 共用 Command DTO 的约束，确保直接调用也受到校验。
     * Lambda 条件使用实体方法引用，不在业务代码中拼写数据库列名。
     */
    @Override
    @Transactional
    public UserRegisterResponseDTO register(UserRegisterCommandDTO command) {
        // 校验
        validate(command);

        // 查重
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, command.getUsername());
        if (userMapper.selectOne(query) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 创建用户
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

    /**
     * 用户登录，不直接返回数据库实体或签发登录令牌。
     *
     * @param command 登录参数
     * @return 登录用户的公开资料
     */
    @Override
    @Transactional
    public UserLoginResponseDTO login(UserRegisterCommandDTO command) {
        //校验格式
        validate(command);
        //查询用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, command.getUsername());
        User user = userMapper.selectOne(query);
        if (user == null) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS); // 账号不存在
        }
        //密码匹配
        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR); // 密码错误（前提：账号存在）
        }
        //生成token
        String token = JwtUtil.createJwtToken(user.getUsername());
        return UserConvert.toUserLoginResponseDTO(user, token); // 转换为登录响应DTO

    }

    /**
     * 校验注册参数。
     *
     * @param command 注册参数
     */
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

    /**
     * 创建用户实体。
     *
     * @param command 注册参数
     * @return 创建的用户实体
     */
    private User createUser(UserRegisterCommandDTO command) {
        User user = new User();
        user.setUsername(command.getUsername());
        user.setPasswordHash(passwordEncoder.encode(command.getPassword())); // 密码哈希
        user.setNickname(command.getUsername()); // 昵称默认与用户名相同
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }

    /**
     * 判断异常是否由唯一键冲突导致。
     *
     * @param exception 可能的唯一键冲突异常
     * @return 是否由唯一键冲突导致
     */
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
