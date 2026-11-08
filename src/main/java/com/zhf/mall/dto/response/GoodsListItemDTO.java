package com.zhf.mall.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 公开商品列表中的单条商品。
 * 数据来自 goods 表，并补充封面图与卖家公开资料，只包含列表展示所需字段。
 */
public record GoodsListItemDTO(
        /** 商品主键，对应 goods.id。 */
        String id,
        /** 商品所属的一级分类标识，对应 categories.id。 */
        String categoryId,
        /** 商品类型标识，与 categoryId 共同确定具体类型，对应 category_types.id。 */
        String typeId,
        /** 商品标题。 */
        String title,
        /** 当前售价，单位为元。 */
        BigDecimal price,
        /** 商品成色编码：NEW、NEAR_NEW、LIKE_NEW、GOOD、USED。 */
        String condition,
        /** 成色对应的中文文案，例如 LIKE_NEW 对应“九五新”。 */
        String conditionLabel,
        /** 封面图片访问地址，取商品图片中展示位置为 1 的那张。 */
        String coverUrl,
        /** 卖家公开信息。 */
        SellerDTO seller,
        /** 卖家类型，目前固定为 PERSONAL（个人闲置）。 */
        String sellerType,
        /** 商品所在城市或地区。 */
        String location,
        /** 商品累计浏览次数。 */
        int viewCount,
        /** 商品状态：ON_SALE 在售、RESERVED 订单占用、SOLD 已售、OFF_SHELF 已下架。 */
        String status,
        /** 商品创建时间，UTC。 */
        Instant createdAt,
        /** 商品最后更新时间，UTC。 */
        Instant updatedAt
) {
    /** 商品列表中的卖家公开信息。 */
    public record SellerDTO(
            /** 卖家用户主键，对应 user.id。 */
            String id,
            /** 卖家公开显示的昵称。 */
            String nickname,
            /** 卖家头像访问地址，可为 null。 */
            String avatarUrl
    ) {}
}
