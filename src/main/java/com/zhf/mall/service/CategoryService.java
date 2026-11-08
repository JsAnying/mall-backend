package com.zhf.mall.service;

import com.zhf.mall.dto.response.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryResponseDTO> getCategories();
}
