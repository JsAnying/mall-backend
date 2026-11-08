package com.zhf.mall.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 商品一级分类。 */
@TableName("categories")
@Data
@NoArgsConstructor
public class Categories {

    /** 稳定的分类标识，例如 digital；由业务提供，不是自增 ID。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** 分类名称 */
    private String name;

    /** 分类图标 */
    private String icon;

    /** 分类入口简短说明 */
    private String hint;

    /** 显示顺序，数值越小越靠前。 */
    private Integer sortOrder;

    /** 是否启用：1 启用，0 停用。 */
    private Integer enabled;
}
