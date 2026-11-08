package com.zhf.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhf.mall.dto.response.CategoryGroupResponseDTO;
import com.zhf.mall.dto.response.CategoryResponseDTO;
import com.zhf.mall.dto.response.CategoryTypeResponseDTO;
import com.zhf.mall.entity.Categories;
import com.zhf.mall.entity.CategoryGroups;
import com.zhf.mall.entity.CategoryTypes;
import com.zhf.mall.mapper.CategoriesMapper;
import com.zhf.mall.mapper.CategoryGroupsMapper;
import com.zhf.mall.mapper.CatoryTypesMapper;
import com.zhf.mall.service.CategoryService;
import com.zhf.mall.service.convert.CategoryConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {



    @Resource
    private CategoriesMapper categoriesMapper;

    @Resource
    private CategoryGroupsMapper categoryGroupsMapper;

    @Resource
    private CatoryTypesMapper catoryTypesMapper;


    @Override
    public List<CategoryResponseDTO> getCategories() {
            // 查询已启用的一级分类
            List<Categories> categories = categoriesMapper.selectList(
                    new LambdaQueryWrapper<Categories>()
                            .eq(Categories::getEnabled, 1)
                            .orderByAsc(Categories::getSortOrder)
                            .orderByAsc(Categories::getId)
            );

            // 查询所有展示分组
            List<CategoryGroups> groups = categoryGroupsMapper.selectList(
                    new LambdaQueryWrapper<CategoryGroups>()
                            .orderByAsc(CategoryGroups::getSortOrder)
                            .orderByAsc(CategoryGroups::getId)
            );

            // 查询已启用的商品类型
            List<CategoryTypes> types = catoryTypesMapper.selectList(
                    new LambdaQueryWrapper<CategoryTypes>()
                            .eq(CategoryTypes::getEnabled, 1)
                            .orderByAsc(CategoryTypes::getSortOrder)
                            .orderByAsc(CategoryTypes::getId)
            );

            // 将类型按 groupId 分组：分组 ID → 该组下的类型
            Map<Long, List<CategoryTypes>> typesByGroup = types.stream()
                    .collect(Collectors.groupingBy(CategoryTypes::getGroupId));

            // 将展示分组按 categoryId 分组：分类 ID → 该分类下的分组
            Map<String, List<CategoryGroups>> groupsByCategory = groups.stream()
                    .collect(Collectors.groupingBy(CategoryGroups::getCategoryId));

            return categories.stream()
                    .map(category -> {
                        List<CategoryGroupResponseDTO> groupDtos =
                                groupsByCategory
                                        .getOrDefault(category.getId(), List.of())
                                        .stream()
                                        .map(group -> {
                                            List<CategoryTypeResponseDTO> typeDtos =
                                                    typesByGroup
                                                            .getOrDefault(group.getId(), List.of())
                                                            .stream()
                                                            .map(CategoryConvert::toCategoryTypeResponseDTO)
                                                            .toList();
                                            return CategoryConvert.toCategoryGroupResponseDTO(group, typeDtos);
                                        })
                                        .toList();
                        return CategoryConvert.toCategoryResponseDTO(category, groupDtos);
                    })
                    .toList();
    }
}
