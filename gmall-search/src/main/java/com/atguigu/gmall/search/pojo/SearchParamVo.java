package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/*
*  search.gmall.com/search?keyword=手机&brandId=1,2,3&categoryId=225&props=4:8G-16G,5:128G
* &sort=1&priceFrom=1000&priceTo=2000&store=true&pageNum=1
* */
@Data
public class SearchParamVo {

    //检索关键字
    private String keyword;
    //品牌过滤条件
    private List<Long> brandId;
    //分类过滤条件
    private List<Long> categoryId;
    //规格参数过滤条件
    private List<String> props;
    //排序字段 0-默认，根据得分降序排列，1价格降序，2价格升序
    private Integer sort = 0;
    //价格区间过滤
    private Double priceFrom;
    private Double priceTo;
    //是否有货过滤
    private Boolean store;
    //分页
    private Integer pageNum = 1;
    private final Integer pageSize = 20;
}
