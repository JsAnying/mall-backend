package com.zhf.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
