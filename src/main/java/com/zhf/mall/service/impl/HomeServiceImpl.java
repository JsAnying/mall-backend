package com.zhf.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhf.mall.dto.response.HomeRecommendationsDTO;
import com.zhf.mall.entity.HomeBanner;
import com.zhf.mall.entity.HomeHotSearch;
import com.zhf.mall.mapper.HomeBannerMapper;
import com.zhf.mall.mapper.HomeHotSearchMapper;
import com.zhf.mall.service.HomeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeServiceImpl implements HomeService {
    @Resource
    private HomeHotSearchMapper hotSearchMapper;

    @Resource
    private HomeBannerMapper bannerMapper;

    @Override
    public HomeRecommendationsDTO getRecommendations() {
         // 1. 查询启用的热搜，按配置顺序返回
        List<String> hotSearches = hotSearchMapper.selectList(
                new LambdaQueryWrapper<HomeHotSearch>()
                        .eq(HomeHotSearch::getEnabled, true)
                        .orderByAsc(HomeHotSearch::getSortOrder)
                        .orderByAsc(HomeHotSearch::getId)
        ).stream().map(HomeHotSearch::getKeyword).toList();

        // 2. 查询当前启用的 Banner
        // 约定同时只启用一个；没有配置时返回 null
        HomeBanner banner = bannerMapper.selectOne(
                new LambdaQueryWrapper<HomeBanner>()
                        .eq(HomeBanner::getEnabled, true)
                        .last("LIMIT 1")// 约定同时只启用一个；没有配置时返回 null
        );

        HomeRecommendationsDTO.BannerDTO bannerDTO = banner == null
                ? null
                : new HomeRecommendationsDTO.BannerDTO(
                        banner.getTitle(),
                        banner.getSubtitle(),
                        banner.getButtonText(),
                        banner.getImageUrl(),
                        banner.getTargetCategoryId()
                );

        return new HomeRecommendationsDTO(hotSearches, bannerDTO);
    }
}
