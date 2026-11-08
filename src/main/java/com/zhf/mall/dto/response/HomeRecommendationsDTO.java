package com.zhf.mall.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 首页展示数据。
 * record 是 Java 17 支持的数据类型，适合用于只读的接口返回值。
 */
public record HomeRecommendationsDTO(
        List<String> hotSearches,
        BannerDTO banner
) {
    public record BannerDTO(
            String title,
            String subtitle,
            String buttonText,
            String imageUrl,
            String targetCategoryId
    ) {}

}
