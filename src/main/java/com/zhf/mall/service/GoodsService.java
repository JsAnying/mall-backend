package com.zhf.mall.service;


import com.zhf.mall.dto.query.GoodsQueryDTO;
import com.zhf.mall.dto.response.GoodsDetailDTO;
import com.zhf.mall.dto.response.GoodsListItemDTO;
import com.zhf.mall.dto.response.PageResponseDTO;

public interface GoodsService {
    PageResponseDTO<GoodsListItemDTO> getGoodsList(GoodsQueryDTO queryDTO);

    /** 查询详情，viewerUsername 为 null 表示游客访问。 */
    GoodsDetailDTO getGoodsDetail(Long id, String viewerUsername);
}
