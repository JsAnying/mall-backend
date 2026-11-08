package com.zhf.mall.controller;

import com.zhf.mall.common.Result;
import com.zhf.mall.dto.response.CategoryResponseDTO;
import com.zhf.mall.service.CategoryService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api")
@RestController
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 获取分类列表,返回完整分类目录结构
     */
    @GetMapping("/categories")
    public Result<List<CategoryResponseDTO>> getCategories() {
        return Result.success(categoryService.getCategories());
    }
}
