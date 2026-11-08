package com.zhf.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // 构建器模式
/**
 * 一级分类响应DTO
 */
public class CategoryResponseDTO {

    // 主键
    private String id;
    // 分类名称
    private String name;
    //  只能返回前端支持的图标名，不返回可执行 HTML。
    private String icon;
    // 提示
    private String hint;

    private List<CategoryGroupResponseDTO> groups;
}
