package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 首页热搜配置，对应旧版参考结构中的 home_hot_searches。 */
@Data
@NoArgsConstructor
@TableName("home_hot_searches")
public class HomeHotSearch {
    /** 数据库自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyword;
    /** 数值越小越靠前。 */
    private Integer sortOrder;
    /** 1 为启用，0 为停用。 */
    private Integer enabled;
}
