package com.zhf.mall.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * 三级分类类型响应DTO
 */

public class CategoryTypeResponseDTO {

    //  主键ID
    private String id;
    // 分类名称
    private String name;
}
