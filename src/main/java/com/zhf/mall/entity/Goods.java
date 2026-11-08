package com.zhf.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 闲置商品数据库实体，对应 goods 表。 */
@Data
@NoArgsConstructor
@TableName("goods")
public class Goods {

    /** 商品主键，数据库自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布商品的卖家主键，关联 user.id。 */
    private Long sellerId;

    /** 商品所属的一级分类标识，对应 categories.id。 */
    private String categoryId;

    /** 商品类型标识，与 categoryId 共同确定具体类型，对应 category_types.id。 */
    private String typeId;

    /** 商品标题，长度 2～80 个字符。 */
    private String title;

    /** 商品详细描述，长度 10～2000 个字符。 */
    private String description;

    /** 当前售价，单位为元，须大于 0。 */
    private BigDecimal price;

    /** 商品成色编码：NEW、NEAR_NEW、LIKE_NEW、GOOD、USED。 */
    private String conditionCode;

    /** 卖家类型，目前仅支持 PERSONAL（个人闲置）。 */
    private String sellerType;

    /** 商品所在城市或地区，长度 1～50 个字符。 */
    private String location;

    /** 商品累计浏览次数，不小于 0，默认为 0。 */
    private Integer viewCount;

    /** 商品状态：ON_SALE 在售、RESERVED 订单占用、SOLD 已售、OFF_SHELF 已下架；默认 ON_SALE。 */
    private String status;

    /** 当前占用该商品的订单主键；为空表示未被占用。非空时 status 必须为 RESERVED。 */
    private Long reservedOrderId;

    /** 商品数据版本号，用于并发修改校验，初始值为 1。 */
    private Integer version;

    /** 软删除时间，对应 DATETIME(3)，按项目约定以 UTC 存取；为空表示未删除。 */
    private LocalDateTime deletedAt;

    /** 记录创建时间，对应 DATETIME(3)，按项目约定以 UTC 存取。 */
    private LocalDateTime createdAt;

    /** 记录最后更新时间，对应 DATETIME(3)，按项目约定以 UTC 存取。 */
    private LocalDateTime updatedAt;
}
