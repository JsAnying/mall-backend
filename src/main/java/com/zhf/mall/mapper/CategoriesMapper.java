package com.zhf.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhf.mall.entity.Categories;
import org.apache.ibatis.annotations.Mapper;

/** 一级分类的数据库操作。 */
@Mapper
public interface CategoriesMapper extends BaseMapper<Categories> {
}
