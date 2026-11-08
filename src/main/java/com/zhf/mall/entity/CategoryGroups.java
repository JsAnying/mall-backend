package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一级分类下的展示分组。 */
@TableName("category_groups")
@Data
@NoArgsConstructor
public class CategoryGroups {
    /** 数据库自增分组 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属一级分类标识。 */
    private String categoryId;

    /** 展示分组标题。 */
    private String title;

    /** 显示顺序，数值越小越靠前。 */
    private Integer sortOrder;
}
