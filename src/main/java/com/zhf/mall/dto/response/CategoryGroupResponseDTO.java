package com.zhf.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * 二级分类响应DTO
 */
public class CategoryGroupResponseDTO {

    // 分类组标题
    private String title;

    // 分类列表
    private List<CategoryTypeResponseDTO> items;
}
