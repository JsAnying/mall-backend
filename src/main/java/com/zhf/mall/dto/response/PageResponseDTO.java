package com.zhf.mall.dto.response;

import java.util.List;

/** 统一分页响应，page 从 1 开始。 */
public record PageResponseDTO<T>(
        /* 当前页的数据列表。 */
        List<T> records,
        /* 当前页码，从 1 开始。 */
        long page,
        /* 每页条数。 */
        long size,
        /* 符合条件的总记录数。 */
        long total,
        /* 总页数，根据 total 与 size 计算。 */
        long pages
) {}
