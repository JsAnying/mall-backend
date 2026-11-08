package com.zhf.mall.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 商品详情，收藏状态按当前访问者返回。 */
public record GoodsDetailDTO(
        String id,
        String categoryId,
        String typeId,
        String title,
        String description,
        BigDecimal price,
        String condition,
        String conditionLabel,
        String coverUrl,
        List<ImageDTO> images,
        SellerDetailDTO seller,
        String sellerType,
        String location,
        int viewCount,
        String status,
        Instant createdAt,
        Instant updatedAt,
        int version,
        Boolean freeShipping,
        Boolean favorited
) {
    public record ImageDTO(String fileId, String url) {}

    /** 公开卖家摘要，尚未接入的统计字段返回 null。 */
    public record SellerDetailDTO(
            String id,
            String nickname,
            String avatarUrl,
            String creditLabel,
            Instant lastActiveAt,
            Instant joinedAt,
            Long soldCount,
            BigDecimal positiveReviewRate
    ) {}
}
