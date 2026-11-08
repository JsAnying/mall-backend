package com.zhf.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhf.mall.entity.CategoryTypes;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品类型的数据库操作。
 * 数据库主键是 (category_id, id)，查询、更新和删除须同时指定两个字段；
 * 不要使用只按 id 操作的 selectById、updateById 或 deleteById。
 */
@Mapper
public interface CatoryTypesMapper extends BaseMapper<CategoryTypes> {
}
