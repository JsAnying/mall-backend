package com.zhf.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhf.mall.entity.CategoryGroups;
import org.apache.ibatis.annotations.Mapper;

/** 分类展示分组的数据库操作。 */
@Mapper
public interface CategoryGroupsMapper extends BaseMapper<CategoryGroups> {
}
