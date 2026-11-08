package com.zhf.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhf.mall.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {
    /**
     * 批量读取当前页商品的首张图片地址。
     *
     * @param goodsIds 当前页的商品 ID，调用方在空页时跳过此查询
     * @return 已绑定首图的商品 ID 与对应的图片地址
     */
    @Select("""
    <script>
    SELECT gi.goods_id AS goodsId, f.url AS url
    FROM goods_images gi
    JOIN uploaded_files f ON f.id = gi.file_id
    WHERE gi.sort_order = 1
      AND gi.goods_id IN
      <foreach collection="goodsIds" item="id" open="(" separator="," close=")">
        #{id}
      </foreach>
    </script>
    """)
    List<CoverRow> selectCovers(@Param("goodsIds") List<Long> goodsIds);

    /** 封面查询结果，供服务层按商品 ID 查找图片地址。 */
    record CoverRow(Long goodsId, String url) {}

    /** 按展示位置读取详情页的全部图片。 */
    @Select("""
            SELECT gi.file_id AS fileId, f.url, gi.sort_order AS sortOrder
            FROM goods_images gi
            JOIN uploaded_files f ON f.id = gi.file_id
            WHERE gi.goods_id = #{goodsId}
            ORDER BY gi.sort_order ASC
            """)
    List<ImageRow> selectImages(@Param("goodsId") Long goodsId);

    record ImageRow(String fileId, String url, int sortOrder) {}

    /** 检查用户是否已收藏该商品。 */
    @Select("SELECT COUNT(*) FROM goods_favorites WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    long countFavorite(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
