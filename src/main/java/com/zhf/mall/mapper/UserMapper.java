package com.zhf.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.zhf.mall.entity.User;

/** 仅提供数据库操作，通用 CRUD 由 MyBatis-Plus 实现。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
