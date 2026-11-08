package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品类型：数据库主键为 (category_id, id)。MyBatis-Plus 的 @TableId 只识别单字段，
 * 查询、更新和删除必须同时以 categoryId 和 id 为条件，不能使用按单个 ID 的快捷方法。
 */
@TableName("category_types")
@Data
@NoArgsConstructor
public class CategoryTypes {
    /** 所属一级分类标识，联合主键的一部分。 */
    private String categoryId;

    /** 类型标识，仅在同一一级分类内唯一。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 所属展示分组 ID。 */
    private Long groupId;

    /** 类型名称。 */
    private String name;

    /** 显示顺序，数值越小越靠前。 */
    private Integer sortOrder;

    /** 是否启用：1 启用，0 停用。 */
    private Integer enabled;
}
