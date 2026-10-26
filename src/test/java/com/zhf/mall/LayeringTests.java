package com.zhf.mall;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.annotation.Resource;
import java.lang.reflect.Modifier;
import com.zhf.mall.controller.UserController;
import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.dto.response.UserRegisterResponseDTO;
import com.zhf.mall.mapper.UserMapper;
import com.zhf.mall.service.UserService;
import com.zhf.mall.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

/** 防止重构后 Controller 越层访问 Mapper，或直接将实体暴露给前端。 */
class LayeringTests {

    @Test
    void controllerOnlyDependsOnServiceAndReturnsAResponseDto() throws Exception {
        assertThat(UserController.class.getDeclaredFields()).hasSize(1);
        assertThat(UserController.class.getDeclaredFields()[0].getType()).isEqualTo(UserService.class);
        assertThat(UserController.class.getMethod("register", UserRegisterCommandDTO.class)
                .getGenericReturnType().getTypeName()).contains(UserRegisterResponseDTO.class.getName());
    }

    @Test
    void mapperOnlyInheritsDatabaseOperations() {
        assertThat(BaseMapper.class.isAssignableFrom(UserMapper.class)).isTrue();
        assertThat(UserMapper.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    void productionDependenciesUseResourceFieldInjection() {
        for (Class<?> type : new Class<?>[]{UserController.class, UserServiceImpl.class}) {
            for (var field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    assertThat(field.isAnnotationPresent(Resource.class)).as(field.toString()).isTrue();
                }
            }
        }
    }
}
