package com.zhf.mall.controller;

import com.zhf.mall.common.Result;
import com.zhf.mall.dto.response.HomeRecommendationsDTO;
import com.zhf.mall.service.HomeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/home")
@RestController
public class HomeController {


    @Resource
    private HomeService homeService;

    /**
     * 获取首页推荐
     * @return
     */
    @GetMapping("/recommendations")
    public Result<HomeRecommendationsDTO> recommendations() {
        return Result.success(homeService.getRecommendations());
    }
}
