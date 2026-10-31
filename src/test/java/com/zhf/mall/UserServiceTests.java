package com.zhf.mall;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.zhf.mall.exception.BusinessException;
import com.zhf.mall.mapper.UserMapper;
import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.entity.User;
import com.zhf.mall.service.impl.UserServiceImpl;
import com.zhf.mall.util.ValidationUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import com.zhf.mall.common.ResultCode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 注册业务测试，重点验证直接调用、并发冲突和写入失败。 */
class UserServiceTests {
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void initializeValidation() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidation() {
        validatorFactory.close();
    }

    private UserMapper mapper;
    private PasswordEncoder encoder;
    private UserServiceImpl service;

    @BeforeEach
    void setup() {
        mapper = mock(UserMapper.class);
        encoder = mock(PasswordEncoder.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "validationUtil", new ValidationUtil(validatorFactory.getValidator()));
    }

    @Test
    void raceAtTheUniqueConstraintBecomes409() {
        when(encoder.encode("example123")).thenReturn("hash");
        when(mapper.insert(any(User.class))).thenThrow(new DuplicateKeyException(
                "SQL insert failed", new java.sql.SQLIntegrityConstraintViolationException(
                        "Duplicate entry for key 'users.uk_user_username'")));
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("test_user", "example123")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getResultCode()).isEqualTo(ResultCode.USERNAME_EXISTS);
                });
    }

    @Test
    void existingUsernameStopsBeforeEncodingOrInserting() {
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(new User());
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("test_user", "example123")))
                .isInstanceOf(BusinessException.class);
        verify(mapper, never()).insert(any(User.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void directCallsCannotBypassValidation() {
        for (UserRegisterCommandDTO request : new UserRegisterCommandDTO[]{null,
                new UserRegisterCommandDTO(null, "example123"), new UserRegisterCommandDTO("valid_user", null),
                new UserRegisterCommandDTO("valid_user", "密".repeat(25))}) {
            assertThatThrownBy(() -> service.register(request))
                    .isInstanceOfSatisfying(BusinessException.class, exception ->
                            assertThat(exception.getResultCode()).isEqualTo(ResultCode.BAD_REQUEST));
        }
        verifyNoInteractions(mapper, encoder);
    }

    @Test
    void missingKeyOrUnexpectedRowCountCannotReturnSuccess() {
        when(encoder.encode("example123")).thenReturn("hash");
        when(mapper.insert(any(User.class))).thenReturn(1);
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isInstanceOf(IllegalStateException.class);
        when(mapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.<User>getArgument(0).setId(10001L);
            return 0;
        });
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unrelatedDuplicateKeyIsNotReportedAsUsernameConflict() {
        DuplicateKeyException failure = new DuplicateKeyException("Duplicate entry for key PRIMARY");
        when(mapper.insert(any(User.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.register(new UserRegisterCommandDTO("valid_user", "example123")))
                .isSameAs(failure);
    }
}
