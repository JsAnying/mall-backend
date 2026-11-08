package com.zhf.mall.dto.query;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** 公开商品列表的筛选、排序和分页参数。 */
@Data
public class GoodsQueryDTO {
    @Size(max = 100, message = "关键词最多 100 位")
    private String keyword;

    @Size(max = 32, message = "分类 ID 最多 32 位")
    private String categoryId;

    @Size(max = 32, message = "类型 ID 最多 32 位")
    private String typeId;

    @Pattern(regexp = "PERSONAL", message = "卖家类型只支持 PERSONAL")
    private String sellerType;

    @DecimalMin(value = "0", message = "最低价不能小于 0")
    private BigDecimal minPrice;

    @DecimalMin(value = "0", message = "最高价不能小于 0")
    private BigDecimal maxPrice;

    @NotNull(message = "排序方式不能为空")
    @Pattern(regexp = "latest|priceAsc|priceDesc", message = "不支持的排序方式")
    private String sort = "latest";

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码至少为 1")
    private Integer page = 1;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数至少为 1")
    @Max(value = 50, message = "每页条数最多为 50")
    private Integer size = 20;

    /** 在长度校验前去除关键词首尾空白，纯空白按省略关键词处理。 */
    public void setKeyword(String keyword) {
        String trimmed = keyword == null ? "" : keyword.strip();
        this.keyword = trimmed.isEmpty() ? null : trimmed;
    }

    @AssertTrue(message = "请传入实际分类 ID，全部商品请省略分类参数")
    public boolean isCategoryIdValid() {
        return categoryId == null
                || (!categoryId.isBlank() && !"all".equals(categoryId) && !"personal".equals(categoryId));
    }

    /** 检查参数组合，实际的分类与类型归属由服务层查库确认。 */
    @AssertTrue(message = "传入类型 ID 时必须同时提供分类 ID，且类型 ID 不能为空")
    public boolean isTypeSelectionValid() {
        return typeId == null || (categoryId != null && !typeId.isBlank());
    }

    @AssertTrue(message = "价格最多保留两位小数")
    public boolean isPriceScaleValid() {
        return (minPrice == null || minPrice.scale() <= 2)
                && (maxPrice == null || maxPrice.scale() <= 2);
    }

    @AssertTrue(message = "最低价不能大于最高价")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }
}
