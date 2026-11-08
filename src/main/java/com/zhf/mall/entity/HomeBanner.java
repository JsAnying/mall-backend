package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 首页宣传内容，对应旧版参考结构中的 home_banners。 */
@Data
@NoArgsConstructor
@TableName("home_banners")
public class HomeBanner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subtitle;
    private String buttonText;
    private String imageUrl;
    /** 可为空，非空时为一级分类 ID。 */
    private String targetCategoryId;
    private Integer sortOrder;
    /** 1 为启用，0 为停用。 */
    private Integer enabled;
}
