package com.zhf.mall.service.convert;

import com.zhf.mall.dto.response.GoodsDetailDTO;
import com.zhf.mall.dto.response.GoodsListItemDTO;
import com.zhf.mall.dto.response.GoodsListItemDTO.SellerDTO;
import com.zhf.mall.entity.Goods;
import com.zhf.mall.entity.User;
import java.time.ZoneOffset;
import java.util.List;

/** 将已查询的商品和卖家资料组装成列表或详情响应。 */
public final class GoodsConvert {

    public static GoodsListItemDTO toListItem(Goods goods, User seller, String coverUrl) {
        String avatarFileId = seller.getAvatarFileId();
        String avatarUrl = avatarFileId == null ? null : "/api/media/avatars/" + avatarFileId;
        SellerDTO sellerDTO = new SellerDTO(seller.getId().toString(), seller.getNickname(), avatarUrl);
        String condition = goods.getConditionCode();

        return new GoodsListItemDTO(
                goods.getId().toString(), goods.getCategoryId(), goods.getTypeId(),
                goods.getTitle(), goods.getPrice(),
                condition, conditionLabel(condition), coverUrl, sellerDTO,
                goods.getSellerType(), goods.getLocation(), goods.getViewCount(), goods.getStatus(),
                goods.getCreatedAt().toInstant(ZoneOffset.UTC),
                goods.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }

    /**
     * 将商品和卖家资料组装成详情响应。
     */
    public static GoodsDetailDTO toDetail(
            Goods goods, User seller, List<GoodsDetailDTO.ImageDTO> images, Boolean favorited) {
        GoodsListItemDTO base = toListItem(goods, seller, images.get(0).url());
        SellerDTO publicSeller = base.seller();
        var detailSeller = new GoodsDetailDTO.SellerDetailDTO(
                publicSeller.id(), publicSeller.nickname(), publicSeller.avatarUrl(),
                null, null, null, null, null);

        return new GoodsDetailDTO(
                base.id(), base.categoryId(), base.typeId(), base.title(), goods.getDescription(),
                base.price(), base.condition(), base.conditionLabel(), base.coverUrl(), images,
                detailSeller, base.sellerType(), base.location(), base.viewCount(), base.status(),
                base.createdAt(), base.updatedAt(), goods.getVersion(), null, favorited);
    }

    private static String conditionLabel(String code) {
        return switch (code) {
            case "NEW" -> "全新";
            case "NEAR_NEW" -> "99新";
            case "LIKE_NEW" -> "95新";
            case "GOOD" -> "9成新";
            case "USED" -> "有使用痕迹";
            default -> throw new IllegalStateException("未知商品成色：" + code);
        };
    }
}
