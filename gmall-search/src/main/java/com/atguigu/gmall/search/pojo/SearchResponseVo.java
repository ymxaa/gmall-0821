package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {
    //品牌列表的渲染所需字段
    private List<BrandEntity> brands;
    //分类列表的渲染所需字段
    private List<CategoryEntity> categories;
    //规格参数的渲染所需字段
    private List<SearchResponseAttrVo> filters;
    //分页所需的数据
    private Integer pageNum;
    private Integer pageSize;
    //总记录数
    private Long total;
    //当前页的商品数据
    private List<Goods> goodsList;
}
