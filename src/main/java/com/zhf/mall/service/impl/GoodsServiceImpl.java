package com.zhf.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhf.mall.common.ResultCode;
import com.zhf.mall.dto.query.GoodsQueryDTO;
import com.zhf.mall.dto.response.GoodsDetailDTO;
import com.zhf.mall.dto.response.GoodsListItemDTO;
import com.zhf.mall.dto.response.PageResponseDTO;
import com.zhf.mall.entity.Categories;
import com.zhf.mall.entity.CategoryTypes;
import com.zhf.mall.entity.Goods;
import com.zhf.mall.entity.User;
import com.zhf.mall.exception.BusinessException;
import com.zhf.mall.mapper.CategoriesMapper;
import com.zhf.mall.mapper.CatoryTypesMapper;
import com.zhf.mall.mapper.GoodsMapper;
import com.zhf.mall.mapper.UserMapper;
import com.zhf.mall.service.GoodsService;
import com.zhf.mall.service.convert.GoodsConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl implements GoodsService {

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private CategoriesMapper categoriesMapper;

    @Resource
    private CatoryTypesMapper catoryTypesMapper;

    @Resource
    private UserMapper userMapper;
    /**
     * 根据查询参数获取商品列表
     *
     * @param query 查询参数
     * @return 商品列表
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<GoodsListItemDTO> getGoodsList(GoodsQueryDTO query) {

        // 分类启用状态和类型归属依赖数据库记录，由服务层校验。
        validateCategory(query);
        // 构建分页查询
        Page<Goods> page = goodsMapper.selectPage(
            new Page<>(query.getPage(), query.getSize()),
            buildGoodsQuery(query));

        // 空页跳过卖家和封面查询，避免生成空的 IN 条件。
        List<GoodsListItemDTO> records = page.getRecords().isEmpty()
                ? List.of()
                : loadListItems(page.getRecords());

        return new PageResponseDTO<>(
                records, page.getCurrent(), page.getSize(),
                page.getTotal(), page.getPages());
    }

    /**
     * 根据商品ID获取商品详情
     *
     * @param id 商品ID
     * @param viewerUsername 浏览器用户名
     * @return 商品详情
     */
    @Override
    @Transactional(readOnly = true)
    public GoodsDetailDTO getGoodsDetail(Long id, String viewerUsername) {
        // 获取当前用户ID
        Long viewerId = findViewerId(viewerUsername);
        Goods goods = goodsMapper.selectOne(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getId, id)
                .isNull(Goods::getDeletedAt));
        // 如果商品不存在或商品详情对当前用户不可见，则抛出商品不存在异常
        if (goods == null || !isDetailVisible(goods, viewerId)) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }

        // 获取卖家信息
        User seller = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getNickname, User::getAvatarFileId)
                .eq(User::getId, goods.getSellerId()));
        // 获取商品图片
        List<GoodsMapper.ImageRow> rows = goodsMapper.selectImages(id);
        if (seller == null || rows.isEmpty() || rows.get(0).sortOrder() != 1) {
            throw new IllegalStateException("商品缺少卖家或首图，goodsId=" + id);
        }

        // 构建图片列表
        List<GoodsDetailDTO.ImageDTO> images = rows.stream()
                .map(row -> new GoodsDetailDTO.ImageDTO(row.fileId(), row.url()))
                .toList();
        // 判断当前用户是否已收藏此商品
        Boolean favorited = viewerId == null ? null : goodsMapper.countFavorite(viewerId, id) > 0;
        // 构建商品详情
        return GoodsConvert.toDetail(goods, seller, images, favorited);
    }

    /**
     * 根据用户名获取用户ID
     *
     * @param viewerUsername 用户名
     * @return 用户ID
     */
    private Long findViewerId(String viewerUsername) {
        if (viewerUsername == null) {
            return null;
        }
        User viewer = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .select(User::getId)
                .eq(User::getUsername, viewerUsername));
        if (viewer == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return viewer.getId();
    }

    /**
     * 判断商品详情是否对当前用户可见
     *
     * @param goods 商品
     * @param viewerId 浏览器用户ID
     * @return 商品详情是否对当前用户可见
     */
    private boolean isDetailVisible(Goods goods, Long viewerId) {
        return switch (goods.getStatus()) {
            case "ON_SALE", "RESERVED", "SOLD" -> true;
            case "OFF_SHELF" -> goods.getSellerId().equals(viewerId);
            default -> false;
        };
    }

    /**
     * 校验分类是否存在且已启用
     *
     * @param queryDTO
     */
    private void validateCategory(GoodsQueryDTO queryDTO) {
        // 获取分类ID
        String categoryId = queryDTO.getCategoryId();
        // 如果分类ID为空，则不进行校验
        if (categoryId == null) {
            return;
        }

        // 校验分类是否存在且已启用
        boolean categoryExists = categoriesMapper.exists(
                new LambdaQueryWrapper<Categories>()
                        .eq(Categories::getId, categoryId)
                        .eq(Categories::getEnabled, 1));
        if (!categoryExists) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类不存在或已停用");
        }

        // 校验类型是否存在且已启用
        String typeId = queryDTO.getTypeId(); // 获取类型ID
        if (typeId != null && !catoryTypesMapper.exists(
                new LambdaQueryWrapper<CategoryTypes>()
                        .eq(CategoryTypes::getCategoryId, categoryId)
                        .eq(CategoryTypes::getId, typeId)
                        .eq(CategoryTypes::getEnabled, 1))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该分类下没有此启用类型");
        }
    }

    /**
     * 构建商品查询条件
     * 根据筛选参数构建在售商品的分页查询条件。
     * @param query
     * @return
     */
    private LambdaQueryWrapper<Goods> buildGoodsQuery(GoodsQueryDTO query) {
        var where = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, "ON_SALE")
                .isNull(Goods::getDeletedAt)
                .eq(query.getCategoryId() != null, Goods::getCategoryId, query.getCategoryId())
                .eq(query.getTypeId() != null, Goods::getTypeId, query.getTypeId())
                .eq(query.getSellerType() != null, Goods::getSellerType, query.getSellerType())
                .ge(query.getMinPrice() != null, Goods::getPrice, query.getMinPrice())
                .le(query.getMaxPrice() != null, Goods::getPrice, query.getMaxPrice());

        // 如果存在关键词
        if (query.getKeyword() != null) {
            // 转义关键词（如果其中包含通配符或转义符 例如 ! % _）
            String escaped = query.getKeyword()
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            String pattern = "%" + escaped + "%"; // 构造 %pattern% 前后加 % 表示"包含"匹配（子串匹配）

            // 添加关键词匹配条件
            where.and(search -> search
                    .apply("title LIKE {0} ESCAPE '!'", pattern)
                    .or()
                    .apply("description LIKE {0} ESCAPE '!'", pattern));
        }

        // 根据排序方式添加排序条件
        switch (query.getSort()) {
            case "latest" -> where.orderByDesc(Goods::getCreatedAt);
            case "priceAsc" -> where.orderByAsc(Goods::getPrice);
            case "priceDesc" -> where.orderByDesc(Goods::getPrice);
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的排序方式");
        }
        return where.orderByDesc(Goods::getId); //最终兜底排序
    }

    /** 批量补齐当前页的卖家和封面，并保留分页查询的商品顺序。 */
    private List<GoodsListItemDTO> loadListItems(List<Goods> goodsList) {
        List<Long> sellerIds = goodsList.stream().map(Goods::getSellerId).distinct().toList();
        Map<Long, User> sellersById = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getNickname, User::getAvatarFileId)
                        .in(User::getId, sellerIds))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<Long> goodsIds = goodsList.stream().map(Goods::getId).toList();
        Map<Long, String> coversById = goodsMapper.selectCovers(goodsIds).stream()
                .collect(Collectors.toMap(GoodsMapper.CoverRow::goodsId, GoodsMapper.CoverRow::url));

        return goodsList.stream().map(goods -> {
            User seller = sellersById.get(goods.getSellerId());
            String coverUrl = coversById.get(goods.getId());
            if (seller == null || coverUrl == null) {
                throw new IllegalStateException("商品缺少卖家或封面，goodsId=" + goods.getId());
            }
            return GoodsConvert.toListItem(goods, seller, coverUrl);
        }).toList();
    }
}
