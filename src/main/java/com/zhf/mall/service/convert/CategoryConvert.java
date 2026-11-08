package com.zhf.mall.service.convert;


import com.zhf.mall.dto.response.CategoryGroupResponseDTO;
import com.zhf.mall.dto.response.CategoryResponseDTO;
import com.zhf.mall.dto.response.CategoryTypeResponseDTO;
import com.zhf.mall.entity.Categories;
import com.zhf.mall.entity.CategoryGroups;
import com.zhf.mall.entity.CategoryTypes;

import java.util.List;

//商品转换类
public class CategoryConvert {

    /**
     * 三级分类响应DTO
     * @param type
     * @return
     */
    public static CategoryTypeResponseDTO toCategoryTypeResponseDTO(CategoryTypes type) {
        return CategoryTypeResponseDTO.builder()
                .id(type.getId())
                .name(type.getName())
                .build();
    }


    /**
     * 二级分类响应DTO
     * @param group
     * @return
     */
    public static CategoryGroupResponseDTO toCategoryGroupResponseDTO(
            CategoryGroups group,
            List<CategoryTypeResponseDTO> items
    ) {
        return CategoryGroupResponseDTO.builder()
                .title(group.getTitle())
                .items(items)
                .build();
    }

    /**
     * 一级分类响应DTO
     * @param category
     * @return
     */
    public static CategoryResponseDTO toCategoryResponseDTO(
            Categories category,
            List<CategoryGroupResponseDTO> groups
    ) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .hint(category.getHint())
                .groups(groups)
                .build();
    }

}
