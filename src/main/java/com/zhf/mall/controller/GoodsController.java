package com.zhf.mall.controller;

import com.zhf.mall.common.Result;
import com.zhf.mall.dto.query.GoodsQueryDTO;
import com.zhf.mall.dto.response.GoodsDetailDTO;
import com.zhf.mall.dto.response.GoodsListItemDTO;
import com.zhf.mall.dto.response.PageResponseDTO;
import com.zhf.mall.service.GoodsService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
    @Resource
    private GoodsService goodsService;

    @GetMapping
    public Result<PageResponseDTO<GoodsListItemDTO>> getGoodsList(@Valid @ModelAttribute GoodsQueryDTO queryDTO) {
        return Result.success(goodsService.getGoodsList(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<GoodsDetailDTO> getGoodsDetail(@PathVariable("id") Long id, Authentication auth) {
        String viewerUsername = auth == null ? null : auth.getName();
        return Result.success(goodsService.getGoodsDetail(id, viewerUsername));
    }
}
